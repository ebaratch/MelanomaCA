package Models.Raf.ContactInhibition;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GuiWindow;
import Framework.Gui.Vis2DOpenGL;
import Framework.Tools.GenomeInfo;
import Framework.Tools.GenomeTracker;
import Framework.Gui.TickTimer;
import Framework.Utils;

import static Framework.Utils.*;

import java.util.ArrayList;
import java.util.Random;

class OLGenome extends GenomeInfo {
    double[]weights=new double[2];
    double mutDist;
    VTOffLattice myTissue;

    public OLGenome(GenomeInfo parent,double w1,double w2,VTOffLattice myTissue) {
        super(parent);
        weights[0]=w1;
        weights[1]=w2;
        this.myTissue=myTissue;
    }

    public boolean RunPossibleMutation(OLCell c) {
        Random rn=myTissue.rn;
        if(myTissue.perturbing&&rn.nextDouble()<myTissue.pointMutProb){
            int iWeight=rn.nextInt(2);
            OLGenome mutantGenome=new OLGenome(this,weights[0],weights[1],myTissue);
            c.myGenome=mutantGenome;
            mutantGenome.weights[iWeight]=Gaussian(mutantGenome.weights[iWeight],myTissue.mutStdDev,rn);
            mutantGenome.mutDist=Math.sqrt(Utils.DistSquared(weights,myTissue.progenitor.weights));
            return true;
        }
        return false;
    }

}

class OLCell extends SphericalAgent2D<OLCell,VTOffLattice> {
    OLGenome myGenome;
    double forceSum;
    double xVel;
    double yVel;
    void Init(OLGenome myGenome){
        this.myGenome=myGenome;
        myGenome.IncPop();
        this.forceSum=0;
        this.radius=G().cellRad;
        this.xVel=0;
        this.yVel=0;
    }
    void Think(){
        double[]ns=G().neurons;
        NNset(ns,G().nIn,ns.length,0.0);
        ns[G().iBias]=G().biasVal;
        ns[G().iHood]=forceSum*G().thinkForceScale;
        NNfullyConnectedLayer(ns,myGenome.weights,0,G().nIn,G().nIn,ns.length,0);
        if(G().stochasticOutput){
            double exp=Math.exp(ns[G().nIn]*G().sigmoidScale);
            ns[G().nIn]=exp/(1+exp);
        }
    }
    void Wait(){ }
    void Apoptosis(){
        myGenome.DecPop();
        Dispose();
    }
    void CellDiv(){
        OLCell child=Divide(G().divRad,G().divCoords,G().rn);
        child.Init(myGenome);
        myGenome.RunPossibleMutation(this);
        child.myGenome.RunPossibleMutation(child);
    }
    void Act(){
//        if(forceSum==0){
//            Divide(G().divRad);
//        }
//        else{Wait();}
        double nnOut=G().neurons[G().nIn];
        if (G().stochasticOutput) {
            if (G().rn.nextDouble() > nnOut) { CellDiv(); }
            else { Wait(); }
        }
        else {
            if (nnOut < 0) { CellDiv(); }
            else { Wait(); }
        }
        ApplyFriction(G().friction);
        ForceMove(true,true);
    }
}


//@FunctionalInterface
//interface RadToForceMap{
//    double DistToForce(double rad);
//}

public class VTOffLattice extends AgentGrid2D<OLCell> {

    TickTimer trt;
    Random rn;
    final GenomeTracker<OLGenome> gt;
    OLGenome progenitor;

    int mutantCount;
    boolean running;
    boolean perturbing;
    final double[] divCoords;

    final boolean radiationMut;
    final int nIn;
    final int nOut;
    final int deathCt;
    final double mutStdDev;
    final double randDeathProb;
    final double pointMutProb;
    final int beginPerturb;
    final boolean stochasticOutput;
    final int iBias;
    final int iHood;
    final int woundFreq;
    final double woundRad;
    final double woundRadSq;
    final double[] neurons;
    final double[] progenitorWeights;
    final double sigmoidScale;
    final double biasVal;
    final String recMutOutPath;
    final ArrayList<OLCell> cellList;
    final double tickRate;
    final double interactionRad;

    final double cellRad;
    final double divRad;
    final Vis2DOpenGL vis;
    final float[] circleCoords;
    final double friction;
    final double forceExp;
    final double forceMul;
    final double thinkForceScale;
    final int visColor;

    public VTOffLattice(GuiWindow set, double w1, double w2, boolean vis) {

        super(set.GetInt("runSize"),set.GetInt("runSize"),OLCell.class,true,true);
        divCoords=new double[2];
        radiationMut = set.GetBool("RadiationMut");
        trt = new TickTimer();
        running = false;
        perturbing = false;
        rn = new Random();
        nIn = 2;
        nOut = 1;
        deathCt = 0;
        mutStdDev = set.GetDouble("point mutation stdDev");
        randDeathProb = set.GetDouble("randDeathProb");
        pointMutProb = set.GetDouble("point mutation prob");
        beginPerturb = set.GetInt("Begin Perturb");
        stochasticOutput = set.GetBool("StochasticOutput");
        mutantCount = 0;
        iBias = 0;
        iHood = 1;
        sigmoidScale = set.GetDouble("SigmoidScale");
        recMutOutPath = set.GetString("MutationsOutFile");
        neurons = new double[nIn + nOut];
        woundFreq = set.GetInt("WoundFreq");
        woundRad = set.GetDouble("WoundRad");
        woundRadSq=woundRad*woundRad;
        progenitorWeights = new double[]{w1, w2};
        biasVal = set.GetDouble("BiasValue");
        OLGenome progenitor=new OLGenome(null,w1,w2,this);
        gt=new GenomeTracker<OLGenome>(progenitor,true);
        cellList=new ArrayList<>();
        cellRad=set.GetDouble("cellRad");
        interactionRad=cellRad*2;
        divRad=cellRad*set.GetDouble("divRad");
        friction=set.GetDouble("friction");
        circleCoords=GenCirclePoints((float)cellRad,10);
        this.vis=vis?new Vis2DOpenGL("Tissue", 1000,1000,xDim,yDim, true):null;
        tickRate=set.GetDouble("viewer timestep")*1000;
        forceExp=set.GetDouble("forceExp");
        forceMul=set.GetDouble("forceMul");
        thinkForceScale=set.GetDouble("thinkForceScale");
        visColor=set.GetInt("VisColor");
    }
    void Step(){
        trt.TickPause((long)tickRate);
        if(vis!=null){
            vis.Clear(RGB((float) 0, (float) 0, (float) 0));
        }
        if(perturbing&&woundFreq!=0&& GetTick()%woundFreq==0){
            Wound();
        }
        for (OLCell c : this) {
            c.forceSum=c.SumForces(interactionRad,cellList,(overlap)->{return Math.abs(Math.pow(overlap/(interactionRad),forceExp))*forceMul;});
        }
        for(OLCell c:this){
            if(perturbing&&rn.nextDouble()<randDeathProb){
                c.Apoptosis();
                continue;
            }
            c.Think();
            c.Act();
            if(vis!=null) {
                float mut=(float)c.myGenome.mutDist;
                float force=(float)(c.forceSum*thinkForceScale);
                //vis.FanShape((float) c.Xpt(), (float) c.Ypt(), 1f, circleCoords, mut, force,0.2f);
                switch (visColor){
                    //Mutation Color
                    case 0:
                        float dw1=(float)Math.abs(c.myGenome.weights[0]-progenitor.weights[0]);
                        float dw2=(float)Math.abs(c.myGenome.weights[1]-progenitor.weights[1]);
                        vis.FanShape((float) c.Xpt(), (float) c.Ypt(), (float) 1, circleCoords,RGB(dw1, dw2, 0.2f));
                        break;
                    //Force Color
                    case 1:
                        vis.FanShape((float) c.Xpt(), (float) c.Ypt(), (float) 1, circleCoords,RGB(force/10, 0.2f, 0.2f));
                        break;
                    //Both Color
                    case 2:
                        vis.FanShape((float) c.Xpt(), (float) c.Ypt(), (float) 1, circleCoords,RGB(force/10, mut, 0.2f));
                        break;
                }
            }
        }
        if(vis!=null) {
            vis.Show();
        }
        IncTick();
    }
    void Wound(){
        cellList.clear();
        double x=rn.nextDouble()*xDim;
        double y=rn.nextDouble()*yDim;
        AgentsInRad(cellList,x,y,woundRad);
        for (OLCell c : cellList) {
            if(DistSquared(c.Xpt(),c.Ypt(),x,y)<woundRadSq){
                c.Apoptosis();
            }
        }
    }
    void Run(int duration){
        Reset();
        OLCell first= NewAgentPT(xDim/2.0,yDim/2.0);
        first.Init(progenitor);
       running=true;
        for (int i = 0; i < duration; i++) {
            if(i==beginPerturb){
                perturbing=true;
            }
            if(vis!=null&&vis.CheckClosed()){
                break;
            }
            Step();
        }
        if(vis!=null){
            vis.Dispose();
        }
    }
}
