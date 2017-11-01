package Models.Raf.NewContactInhibition;

import Framework.Gui.Vis2DOpenGL;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.RGB;


/**
 * Created by Rafael on 7/26/2017.
 */
public class TissueRunner {
    static float[]circleCoords= Utils.GenCirclePoints(1,20);
    static int SIDE_LEN=50;
    static int RUN_DURATION=1000000;
    static int STEP_PAUSE=10;
    static int NORM_POP=1000;
    static int MUT_POP=1;
    static final String[] inputNames=new String[]{"MutCellRadProp","G0scaleNorm","G1scaleNorm","G0scaleMut","G1scaleMut","RandDeathProb","ForceConst","ForceExp","FrictionConst","StartNormPop","StartMutPop"};
    static final String[] outputNames=new String[]{"ConfluencyNorm","ConfluencyMut","AvgGrowthToConfNorm","AvgGrowthToConfMut"};
    public void SingleRunVis() {
        Vis2DOpenGL vis=new Vis2DOpenGL("Tissue vis", 1000,1000,SIDE_LEN,SIDE_LEN);
        Tissue t=new Tissue(SIDE_LEN);
        TickTimer trt=new TickTimer();
        t.Setup(NORM_POP,MUT_POP);
        for (int i = 0; i < RUN_DURATION; i++) {
            trt.TickPause(STEP_PAUSE);
            t.Step();
            DrawAll(vis,t);
        }
    }
    static ArrayList<double[]> SetupInputs(int nRuns,Random rn){
        ArrayList<double[]> ret=new ArrayList<>();
        for (int i = 0; i < nRuns; i++) {
            double[]runArgs=new double[9];
            runArgs[0]=rn.nextDouble()*0.5+0.5;//MutCellRadProp
            runArgs[1]=rn.nextDouble()*20+5;//G0scaleNorm
            runArgs[2]=rn.nextDouble()*100+25;//G1scaleNorm should be more than G0scaleNorm
            runArgs[3]=rn.nextDouble()*5;//G0scaleMut should be less than G0scaleNorm
            runArgs[4]=rn.nextDouble()*50+5;//G1scaleMut should be less than G0scaleMut
            runArgs[5]=rn.nextDouble()*0.001;//very small RandDeathProb
            runArgs[6]=rn.nextDouble()*2;//force const
            runArgs[7]=rn.nextDouble()*4;//force exp and const should prevent blowup
            runArgs[8]=rn.nextDouble();//friction also involved in stability
            runArgs[9]=rn.nextDouble()*1000;//lots of normal cells
            runArgs[10]=rn.nextDouble()*20;//fewer mutant cells
            ret.add(runArgs);
        }
        return ret;
    }
    static void InitModel(double[]inputs,Tissue t){
        t.CELL_RAD_MUT=t.CELL_RAD_NORM*inputs[0];
        t.G0_SCALE_NORM=inputs[1];
        t.G1_SCALE_NORM=inputs[2];
        t.G0_SCALE_MUT=inputs[3];
        t.G1_SCALE_MUT=inputs[4];
        t.RAND_DEATH_PROB=inputs[5];
        t.FORCE_CONST=inputs[6];
        t.FORCE_EXP=inputs[7];
        t.FRICTION_CONST=inputs[8];
        t.Setup((int)inputs[9],(int)inputs[10]);
    }
    public void SweepRun(int nRuns,int nThreads){
        Random rn=new Random();
        ArrayList<double[]> inputs=SetupInputs(nRuns,rn);
        ArrayList<RunRes>out=new ArrayList<>(nRuns);
        for (int i = 0; i < nRuns; i++) {
            out.add(null);
        }
        Utils.MultiThread(nRuns,nThreads,(iThread)->{
            RunRes ret=new RunRes(RUN_DURATION);
            Tissue t=new Tissue(SIDE_LEN);
            InitModel(inputs.get(iThread),t);
            for (int iTick = 0; iTick < RUN_DURATION; iTick++) {
                t.Step();
                ret.RecordStep(t);
            }
            ret.GenFinalMetrics();
            out.set(iThread,ret);
        });
    }
    public static void main(String[] args) {
        TissueRunner tr=new TissueRunner();
        tr.SingleRunVis();
    }
    public static void DrawContactInhib(Vis2DOpenGL vis,Tissue t){
        vis.Clear(RGB((float) 0, (float) 0, (float) 0));
        for (Cell c : t) {
            vis.FanShape((float)c.Xpt(), (float)c.Ypt(), (float)(c.radius+0.05), circleCoords,RGB((float) 0, (float) 0, (float) 0));
            vis.FanShape((float)c.Xpt(), (float)c.Ypt(), (float)c.radius, circleCoords,RGB((float)c.forceSum, (float)c.forceSum, 0.2f));
        }
        vis.Show();
    }
    public static void DrawAll(Vis2DOpenGL vis,Tissue t){
        vis.Clear(RGB((float) 0, (float) 0, (float) 0));
        for (Cell c : t) {
            vis.FanShape((float) c.Xpt(), (float) c.Ypt(), (float) (c.radius), circleCoords,RGB((float) c.forceSum, (float) c.forceSum, c.cycleCt * 1.0f / c.Genome.cellCycle));
        }
        for (Cell c : t) {
            float r=0; float g=0; float b=0;
            if(c.IsMut()){ r=1; }
            else{ g=1; }
            vis.FanShape((float)c.Xpt(), (float)c.Ypt(), (float)c.radius/2, circleCoords,RGB(r, g, b));
        }
        vis.Show();
    }
    public static void DrawCellPheno(Vis2DOpenGL vis,Tissue t){
        vis.Clear(RGB((float) 0, (float) 0, (float) 0));
        for (Cell c : t) {
            float r=0; float g=0; float b=0;
            if(c.IsMut()){ r=1; }
            else{ g=1; }
            vis.FanShape((float)c.Xpt(), (float)c.Ypt(), (float)(c.radius+0.05), circleCoords,RGB((float) 0, (float) 0, (float) 0));
            vis.FanShape((float)c.Xpt(), (float)c.Ypt(), (float)c.radius, circleCoords,RGB(r, g, b));
        }
        vis.Show();
    }
    public static void WriteResults(ArrayList<double[]>inputs,ArrayList<RunRes>outputs){}
}

class RunRes{
    final int[] popSizesNorm;
    final int[] popSizesMut;
    double[] outputs;
    RunRes(int nTicks){
        popSizesNorm=new int[nTicks];
        popSizesMut=new int[nTicks];
    }
    void GenFinalMetrics(){
        outputs[0]=GetConfluencyTick(popSizesNorm,50);//confluencyNorm
        outputs[1]=GetConfluencyTick(popSizesMut,50);//confluencyMut
        outputs[2]=GetGrowthRateToConf(popSizesNorm, (int) outputs[0]);//growthRateToConfNorm
        outputs[3]=GetGrowthRateToConf(popSizesMut,(int)outputs[1]);//growthRateToConfMut
    }
    void RecordStep(Tissue t){
        int normPop=0;
        int mutPop=0;
        for (Cell c:t) {
            if(c.IsMut()){ mutPop++; }
            else{ normPop++; }
        }
        popSizesNorm[t.GetTick()]=normPop;
        popSizesMut[t.GetTick()]=mutPop;
    }
    //confluency is when the tissue stops growing (when pop is not greater after 50 ticks)
    int GetConfluencyTick(int[] pops,int checkFreq){
        int prevPop=0;
        for (int i = 0; i < pops.length; i+=checkFreq) {
            if(pops[i]<=prevPop){
                return i;
            }
        }
        return pops.length-1;
    }
    double GetGrowthRateToConf(int[]pops,int confTick){
        double GrowthPropSum=0;
        for (int i = 1; i <= confTick; i++) {
            GrowthPropSum+=pops[i]/pops[i-1];
        }
        return GrowthPropSum/(confTick-1);
    }
}
