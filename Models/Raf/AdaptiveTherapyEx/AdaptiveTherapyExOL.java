package Models.Raf.AdaptiveTherapyEx;

import Framework.Extensions.SphericalAgent3D;
import Framework.GridsAndAgents.*;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.Vis3DOpenGL;
import Framework.Gui.TickTimer;
import Framework.Interfaces.AgentToColorInt;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;


class TissueOL extends AgentGrid3D<CellOL> {
    double TIMESTEP_MULT=1;
    double DIV_PROB_NORM=ProbScale(0.0025,TIMESTEP_MULT);
    double DIV_PROB_RESIST=ProbScale(0.00005,TIMESTEP_MULT);
    double DEATH_PROB_RESIST=ProbScale(0.0001,TIMESTEP_MULT);
    double DEATH_PROB_NORM=ProbScale(0.0001,TIMESTEP_MULT);
    double DEATH_PROB_CONC_SCALE=ProbScale(0.01,TIMESTEP_MULT);
    double RESISTANT_START_PROB=ProbScale(0.5,TIMESTEP_MULT);
    double TREATMENT_START=2000/TIMESTEP_MULT;
    double TREATMENT_PERIOD=1000/TIMESTEP_MULT;
    double TREATMENT_DURATION=300;
    double TREATMENT_DIFF_RATE=0.15;
    double TREATMENT_DECAY=ProbScale(0.98,TIMESTEP_MULT);

    double CELL_RAD=0.5;
    double DIV_RAD=CELL_RAD*2.0/3.0;
    double FORCE_EXP=1;
    double FRICTION=0.05;
    double FORCE_CONST=1*FRICTION;
    double CONTACT_INHIB_THRESH=0.5;

    final int[]neighborhood=MooreHood(false);
    final int[]divIs=new int[neighborhood.length/2];
    final double[]coordScratch=new double[3];
    final ArrayList<CellOL> cellScratch=new ArrayList<>();
    final Random rn=new Random();

    final PDEGrid3D drug;
    TissueOL(int sideLen,double startRad, int startPop){
        super(sideLen,sideLen,sideLen,CellOL.class);
        drug= new PDEGrid3D(sideLen,sideLen,sideLen);
        //int[] startIs=RandomIndices(length,startPop,rn);
        for (int i = 0; i < startPop; i++) {
            RandomPointInSphere(startRad, coordScratch, rn);
            coordScratch[0]+=xDim/2.0;
            coordScratch[1]+=yDim/2.0;
            coordScratch[2]+=zDim/2.0;
            if(In(coordScratch[0],coordScratch[1],coordScratch[2])) {
                CellOL c = NewAgentPT(coordScratch[0], coordScratch[1], coordScratch[2]);
                c.Init(rn.nextDouble() < RESISTANT_START_PROB);
            }
        }
    }
    void Step(){
        Treatment();
        for (CellOL c : this) {
            c.ThinkStep();
        }
        for (CellOL c : this) {
            c.ActStep();
        }
        CleanShuffInc(rn);
    }
    void Treatment(){
        for (CellOL c:this) {
            drug.Mul(c.Isq(),TREATMENT_DECAY);
        }
        if(GetTick()>TREATMENT_START&&GetTick()%TREATMENT_PERIOD<TREATMENT_DURATION) {
            drug.Diffusion(TREATMENT_DIFF_RATE, (double) 1);
        }
        else {
            drug.Diffusion(TREATMENT_DIFF_RATE, (double) 0);
        }
    }
}

class CellOL extends SphericalAgent3D<CellOL,TissueOL> {
    boolean resistant;
    double forceSum;

    void Init(boolean resistant){
        this.resistant=resistant;
        this.radius=G().CELL_RAD;
        this.xVel=0;
        this.yVel=0;
    }
    CellOL Divide(){
        CellOL daughter=Divide(G().DIV_RAD,G().coordScratch,G().rn);
        daughter.Init(resistant);
        this.Init(resistant);
        return daughter;
    }
    double CalcDivProb(){
        if(resistant){
            return G().DIV_PROB_RESIST;
        }
        else{
            return G().DIV_PROB_NORM;
        }
    }
    double CalcDeathProb(){
        if(resistant){
            return G().DEATH_PROB_RESIST;
        }
        else{
            return G().DEATH_PROB_NORM+(G().drug.Get(Isq())*G().DEATH_PROB_CONC_SCALE);
        }
    }
    double OverlapResponse(double overlap){
        //if(overlap<0){ return -0.0001*G().FRICTION; }
        //if(overlap<0){ return 0; }
            return Math.pow(overlap*G().FORCE_CONST,G().FORCE_EXP);
    }
    void ThinkStep(){
        forceSum=SumForces(G().CELL_RAD*2,G().cellScratch,this::OverlapResponse);
    }
    void ActStep(){
        ApplyFriction(G().FRICTION);
        ForceMove();
        if(G().rn.nextDouble()<CalcDeathProb()){
            Dispose();
            return;
        }
        if(forceSum<G().CONTACT_INHIB_THRESH&&G().rn.nextDouble()<CalcDivProb()){
            Divide();
        }
    }
}


public class AdaptiveTherapyExOL {
    static int SIDE_LEN=20;
    static int TIMESTEPS=100000;
    static int START_POP=10;
    static double START_RAD=SIDE_LEN/50.0;
    static int TICK_PAUSE=0;
    static float[] circlePts=GenCirclePoints(1,20);
    public static void main(String[] args) {
        Vis3DOpenGL vis3D=new Vis3DOpenGL("3D Cells", 1000,1000,SIDE_LEN,SIDE_LEN,SIDE_LEN, true);
        GuiWindow win=new GuiWindow("Paper Example",true);
        GuiGridVis visCells=new GuiGridVis(SIDE_LEN,SIDE_LEN,10);
        GuiGridVis visDrug=new GuiGridVis(SIDE_LEN,SIDE_LEN,10);
        TissueOL t=new TissueOL(SIDE_LEN,START_RAD,START_POP);
        TickTimer trt=new TickTimer();
        win.AddCol(0, visCells);
        win.AddCol(1, visDrug);
        win.RunGui();
        for (int i = 0; i < TIMESTEPS; i++) {
            trt.TickPause(TICK_PAUSE);
            t.Step();
            DrawAll(t,visCells,visDrug,vis3D);
        }
        win.Dispose();
    }
    public static void DrawAll(TissueOL t,GuiGridVis visCells,GuiGridVis visDrug,Vis3DOpenGL vis3D){
        vis3D.Clear(RGB((float) 0, (float) 0, (float) 0));
        for (CellOL c : t) {
            vis3D.CelSphere(c.Xpt(), c.Ypt(), c.Zpt(), c.radius,RGB((double) 1, (double) 0, (double) 0));
            //                vis3D.FanShape(c.Xpt(), c.Ypt(), c.Zpt() + 0.0001, c.radius + 0.05, circlePts, 0, 0, 0);
//            vis3D.FanShape(c.Xpt()+0.2, c.Ypt()+0.2, c.Zpt() - 0.0001, c.radius * 0.2, circlePts, 1, 1, 1);
//                if (c.resistant) {
//                    vis3D.FanShape(c.Xpt(), c.Ypt(), c.Zpt(), c.radius, circlePts, 1, 0, c.Zpt() / t.zDim);
//                } else {
//                    vis3D.FanShape(c.Xpt(), c.Ypt(), c.Zpt(), c.radius, circlePts, 0, 1, c.Zpt() / t.zDim);
//            }
        }
        vis3D.Show();
        AgentToColorInt<CellOL> colorFn = (CellOL c)->{
            if(c.resistant){
                return RGB(1,0,0);
            }
            else{
                return RGB(0,1,0);
            }
        };
        visCells.DrawClosestAgentsXY(t, colorFn, RGB((double) 0, (double) 0, (double) 0));
        visDrug.DrawGridDiffXY(t.drug,(val)-> HeatMapRBG(val, (double) 0, (double) 1));
    }
}
