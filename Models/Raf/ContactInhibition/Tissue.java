package Models.Raf.ContactInhibition;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

class Cell extends AgentSQ2Dunstackable<Tissue> {
    int mutantID;
    double[] weights;
    int nDivOptions;
    void init(double[] parentWeights,int parentID) {
        mutantID = parentID;
        if (weights == null) {
            weights = new double[parentWeights.length];
        }
        System.arraycopy(parentWeights, 0, weights, 0, parentWeights.length);
        if (G().visActions != null) {
            float g = (G().nIn>1)? WeightColor(1):0f;
            float b = (G().nIn>2)?WeightColor(2):0f;
            G().visGeno.SetPix(Xsq(), Ysq(), RGB(WeightColor(0),g,b));
            G().visActions.SetPix(Xsq(), Ysq(), RGB((double) 0f, (double) 1f, (double) 0f));
        }
    }

    void PointMutation() {
        if (G().perturbing && G().pointMutProb > 0 && G().rn.nextDouble() < G().pointMutProb) {
            int iMut = G().rn.nextInt(weights.length);
            G().Mutate(weights, iMut, G().pointMutStdDev);
            G().mutantCount += 1;
            mutantID = G().mutantCount;
            //code for recording mutants goes here
        }
    }

    float WeightColor(int iWeight){
        return (float)(Math.abs((weights[iWeight] - G().progenitorWeights[iWeight])) / G().weightRange)+0.1f;
    }

    int GetLocalEmpty(){
        G().HoodToIs(G().localCS, G().localIS, Xsq(), Ysq(), true, true);
        int ct = 0;
        for (int i : G().localIS) {
            Cell c = G().GetAgent(i);
            if (c == null) {
                G().localIS[ct]=i;
                ct++;
            }
        }
        return ct;
    }

    int GetFurtherOcc(){
        G().HoodToIs(G().furtherCS, G().furtherIS, Xsq(), Ysq(), true, true);
        int ct = 0;
        for (int i : G().furtherIS) {
            Cell c = G().GetAgent(i);
            if (c != null) {
                ct++;
            }
        }
        return ct;
    }

    void Think() {
        double[] ns = G().neurons;
        NNset(ns, G().nIn, G().neurons.length, 0.0);
        if (G().iBias != -1) {
            ns[G().iBias] = G().biasVal;
        }
        nDivOptions=GetLocalEmpty();
        int nNeighbors=8-nDivOptions;
        if (G().iLocal != -1) {
            ns[G().iLocal] = (nNeighbors / G().localIS.length);
        }
        if (G().iFurther != -1) {
            ns[G().iFurther] = ((GetFurtherOcc()+nNeighbors)*1.0 / (G().furtherIS.length+G().localIS.length));
        }
        NNfullyConnectedLayer(ns,weights,0,G().nIn,G().nIn,G().nIn+G().nOut,0);
        if (G().stochasticOutput) {
            double exp=Math.exp(ns[G().nIn]*G().sigmoidScale);
            ns[G().nIn] = exp/(1+exp);
        }
    }

    void Act() {
        double nnOut = G().neurons[G().nIn];//nIn is actually the output index!
        if (G().stochasticOutput) {
            if (G().rn.nextDouble() > nnOut) {
                Divide();
            } else {
                Wait();
            }
        } else {
            if (nnOut < 0) {
                Divide();
            } else {
                Wait();
            }
        }
    }

    void Divide() {
        if (G().visActions != null) {
            //G().visGeno.SetColor(Xsq(), Ysq(),WeightColor(weights[0]),WeightColor(weights[1]),0.2f);
            G().visActions.SetPix(Xsq(), Ysq(), RGB((double) 0f, (double) 1f, (double) 0f));
        }
        int iDiv;
        if (nDivOptions == 0) {
            return;
        }
        if (nDivOptions == 1) {
            iDiv = G().localIS[0];
        }
        else {
            iDiv = G().localIS[G().rn.nextInt(nDivOptions)];
        }
        Cell c = G().NewAgentSQ(iDiv);
        c.init(weights, mutantID);
        if (G().perturbing&&!G().radiationMut) {
            c.PointMutation();
            PointMutation();
        }
        //G().heuristic += G().divHeuristicVal
    }

    void Apop() {
        if (G().visActions != null) {
            G().visActions.SetPix(Xsq(), Ysq(), RGB((double) 0f, (double) 0f, (double) 0f));
            G().visGeno.SetPix(Xsq(), Ysq(), RGB((double) 0f, (double) 0f, (double) 0f));
        }
        G().deathCt++;
        Dispose();
    }

    void Wait() {
        if (G().visActions != null) {
            //G().visActions.SetColor(Xsq(), Ysq(), WeightColor(weights[0]),WeightColor(weights[1]), 0.2f);
            G().visActions.SetPix(Xsq(), Ysq(), RGB((double) 0f, (double) 0f, (double) 1f));
        }
        //exponential heuristic value depending on local hood
        //G().heuristic += G().waitHeuristicVal * Math.pow(nHeuristicOcc * (1.0 / G().divIS.size), G().waitHeuristicExp)
    }

    void Step() {
        if (G().perturbing && G().rn.nextDouble() < G().randDeathProb) {
            Apop();
            return;
        }
        if (G().perturbing&&G().radiationMut) {
            PointMutation();
        }
        Think();
        Act();
    }
}

public class Tissue extends AgentGrid2D<Cell> {

    TickTimer myRateTimer;
    Random rn;
    boolean running;
    int timestep;
    int deathCt;
    boolean boundWeights;
    boolean radiationMut;
    double mutStdDev;
    double pointMutProb;
    double weightMin;
    double weightMax;
    double weightRange;
    double pointMutStdDev;
    double randDeathProb;
    int beginPerturb;
    boolean perturbHomeostasis;
    boolean stochasticOutput;
    boolean perturbing;
    int mutantCount;
    double minStartDensity;
    double maxStartDensity;
    int iBias;
    int iLocal;
    int iFurther;
    int woundFreq;
    int[] woundCS;
    int[] localWoundIS;
    double[] neurons;
    int[]localCS;
    int[]localIS;
    int[]furtherCS;
    int[]furtherIS;
    double[]progenitorWeights;
    boolean recMuts;
    ArrayList<double[]>mutationRecord;
    String recMutOutPath;
    double sigmoidScale;
    double biasVal;
    int nIn;
    int nOut;
    double woundRad;
    GuiGridVis visActions;
    GuiGridVis visGeno;

    Tissue(int xDim, int yDim, double[] progenitorWeights, GuiWindow set, GuiGridVis visActions, GuiGridVis visGeno) {
        super(xDim, yDim, Cell.class);
        myRateTimer = new TickTimer();
        running = false;
        timestep = 0;
        //class members
        rn = new Random();
        //nn params
        deathCt = 0;
        this.visActions=visActions;
        this.visGeno=visGeno;
        boundWeights = set.GetBool("boundWeights");
        radiationMut = set.GetBool("RadiationMut");
        mutStdDev = set.GetDouble("GA mutation stdDev");
        pointMutProb = set.GetDouble("point mutation prob");
        weightMin = set.GetDouble("weight min");
        weightMax = set.GetDouble("weight max");
        weightRange = weightMax - weightMin;
        pointMutStdDev = set.GetDouble("point mutation stdDev");
        //other run params
//    internal val divHeuristicVal = set.GetDouble("divHeuristicValue")
//    internal val waitHeuristicVal = set.GetDouble("waitHeuristicValue")
//    internal val waitHeuristicExp = set.GetDouble("waitHeuristicExp")
        randDeathProb = set.GetDouble("randDeathProb");
        perturbHomeostasis = set.GetBool("PerturbHomeostasis");
        perturbing = false;
        //cell shared data
        stochasticOutput = set.GetBool("StochasticOutput");
//    internal var heuristic: Double = 0.toDouble()
        mutantCount = 0;
        minStartDensity = set.GetDouble("MinStartDensity");
        maxStartDensity = set.GetDouble("MaxStartDensity");
        biasVal = set.GetDouble("BiasValue");
        //setup nn params
        beginPerturb = set.GetInt("Begin Perturb");
        int nCount = 0;
        iBias = (set.GetBool("Bias"))? nCount++ : -1;
        iLocal =(set.GetBool("Local"))? nCount++ : -1;
        iFurther =(set.GetBool("Further"))? nCount++ : -1;
        nIn = nCount;
        woundRad = set.GetDouble("WoundRad");
        nOut = 1;
        this.progenitorWeights = new double[nIn * nOut];
        if (progenitorWeights == null) {
            RandomDS(this.progenitorWeights, -1.0, 1.0, rn);
        }//randomize weights
        else {
            System.arraycopy(progenitorWeights, 0, this.progenitorWeights, 0, this.progenitorWeights.length);
        }

        //read menu params
        if (woundRad > 0) {
            woundCS = CircleHood(true, woundRad);
            localWoundIS = new int[woundCS.length / 2];
        } else {
            woundCS = null;
            localWoundIS = null;
        }
        woundFreq = set.GetInt("WoundFreq");
        recMuts = set.GetBool("RecMutations");
        //if(bRecMuts){
        mutationRecord = new ArrayList<>();
        recMutOutPath = set.GetString("MutationsOutFile");
        sigmoidScale=set.GetDouble("SigmoidScale");
        //}

        //setup cell shared data
        //localCS=Utils.MooreHood();
        localCS = MooreHood(false);
        furtherCS = new int[]{-1, 2, 0, 2, 1, 2, 2, 1, 2, 0, 2, -1, -1, -2, 0, -2, 1, -2, -2, 1, -2, 0, -2, -1};
        localCS = MooreHood(false);
        localIS = new int[localCS.length / 2];
        furtherIS = new int[furtherCS.length / 2];
        localIS = new int[localCS.length / 2];
        neurons = new double[nIn + nOut];

        Reset();
//        heuristic = 0.0
        mutantCount = 0;
        visActions.Clear(RGB((double) 0f, (double) 0f, (double) 0f));
        Cell first = NewAgentSQ(xDim / 2, yDim / 2);
        first.init(this.progenitorWeights, 0);
    }//setup class members

    void Step() {
        if (perturbing && woundCS != null && woundFreq > 0 && GetTick() % woundFreq == 0) {
            Wound();
        }
        for (Cell c : this) {
            c.Step();
        }
        this.CleanShuffInc(rn);
        if (timestep > 0) {
            myRateTimer.TickPause(timestep);
        }
    }

    int Wound() {
        int woundX = rn.nextInt(xDim);
        int woundY = rn.nextInt(yDim);
        HoodToIs(woundCS, localWoundIS, woundX, woundY, true, true);
        int killCt = 0;
        for (int iKillLoc : localWoundIS) {
            Cell c = GetAgent(iKillLoc);
            if (c != null) {
                c.Apop();
                killCt++;
            }
        }
        return killCt;
    }

    void Mutate(double[] weights, int i, double stdDev) {
        weights[i] += Gaussian(0.0, stdDev, rn);
//        if (boundWeights) {
//            weights[i] = Bound(weights[i], weightMin, weightMax)
//        }
    }

    void MutateAll() {
        for (int i=0;i<progenitorWeights.length;i++) {
            Mutate(progenitorWeights, i, mutStdDev);
        }
    }

    void Kill() {
        running = false;
    }

    void RecMuts() {
        double[] wtSums = new double[nIn * 2 + 1];
        int cellCt = 0;
        double maxDist = 0.0;
        wtSums[0] = GetTick();
        for (Cell c : this) {
            for (int j=0;j<c.weights.length;j++){
                wtSums[j * 2 + 1] += c.weights[j];
            }
            double cellDist = Utils.DistSquared(c.weights, progenitorWeights);
            if (cellDist > maxDist) {
                for (int j=0;j<c.weights.length;j++) {
                    wtSums[j * 2 + 2] = c.weights[j];
                    maxDist = cellDist;
                }
            }
            cellCt++;
        }
        for (int j=0;j<nIn;j++){
            wtSums[j * 2 + 1] /= cellCt;
        }
        if (maxDist != 0.0) {
            mutationRecord.add(wtSums);
        }
    }

    double[] MutRecordAverage(){
        double[] ret = new double[nIn];
        for (double[] vals : mutationRecord) {
            for (int i=0;i<nIn;i++){
                ret[i] += vals[i * 2 + 1];
            }
        }
        for (int i=0;i<nIn;i++){
            if (mutationRecord.size() > 0) {
                ret[i] /= 1.0*mutationRecord.size();
            } else {
                ret[i] = progenitorWeights[i];
            }
        }
        return ret;
    }

    double[] Run(int duration, boolean setStep, GuiWindow set){
        running = true;
        timestep =(setStep)?(int)(set.GetDouble("viewer timestep") * 1000): 0;

        double[] ret=new double[4];
        for (int i=0;i<duration;i++){
            double density= GetPop()*1.0/length;
            if(density==0.0||density==.98){
                running=false;
                if(ret[0]==0.0){
                    ret[0]=density;
                    ret[2]=i*1.0;
                }
            }
            if (!running) {
                ret[1]=density;
                ret[3]=i*1.0;
                return ret;
            }
            Step();
            //if(bRecMuts){
            RecMuts();
            //}
            if(perturbHomeostasis) {
                if (i != 0 && !perturbing && density == GetPop() * 1.0 / length) {
                    perturbing = true;
                    ret[0] = density;
                    if (density < minStartDensity || density > maxStartDensity) {
                        running = false;
                    }
                }
            }
            else{
                if(i==beginPerturb){
                    perturbing=true;
                }
            }
        }
        if (recMuts) {
            FileIO mutF = new FileIO(recMutOutPath, "a");
            if (mutF.length() == 0.0) {
                mutF.Write("GetTick");
                if (iBias != -1) {
                    mutF.Write(",AvgBias,FurthestBias");
                }
                if (iLocal != -1) {
                    mutF.Write(",AvgLocal,FurthestLocal");
                }
                if (iFurther != -1) {
                    mutF.Write(",AvgFurther,FurthestFurther");
                }
                mutF.Write("\n");
            }
            for (double[] vals : mutationRecord) {
                mutF.WriteDelimit(vals, ",");
                mutF.Write("\n");
            }
            mutF.Close();
        }
//        return heuristic
        ret[1]= GetPop()*1.0/length;
        ret[3]=duration*1.0;
        return ret;
    }
}

