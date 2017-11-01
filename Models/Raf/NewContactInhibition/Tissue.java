package Models.Raf.NewContactInhibition;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Tools.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by rafael on 7/22/17.
 */

/*

model features:
    cells must make the decision to either go through interphase and divide, or G0 phase
    cells eat resources to survive
        some amount goes to metabolism
        some amount to cell cycle (if cell is dividing)
        cell can switch to G0 in case of low resources
    model different parts of the cell div process (only if they impact the overall dynamics)
    cells can be either

    each time step is 1 hr
    typeGrid length is 50 microns

    10A: Normal
    DCIS: Mutant

 */

class CellGenome extends GenomeInfo{
    double g0scale;
    double g1scale;
    double startRad;
    double randDeathProb;
    boolean isMut;
    int cellCycle;

    public CellGenome(GenomeInfo parent,double g0scale,double g1scale,double startRad, double randDeathProb,int cellCycle,boolean mut) {
        super(parent);
        this.g0scale=g0scale;
        this.g1scale=g1scale;
        this.startRad=startRad;
        this.randDeathProb=randDeathProb;
        this.cellCycle=cellCycle;
        this.isMut =mut;
    }
}

public class Tissue extends AgentGrid2D<Cell> {
    final Random rn;

    int CELL_CYCLE=3;//in hrs
    int R_CYCLE_CHECK=2;
    int G0_BACKTRACK=1;
    double CELL_RAD_GROWTH_EXTRA=Math.sqrt(2.0/Math.PI)/Math.sqrt(1.0/Math.PI)-1;//growth needed to double cell volume
    double FRICTION_CONST=0.1;
    double CELL_RAD_NORM=0.4;//20 microns min
    double CELL_RAD_MUT=0.4;//10 microns min
    double G0_SCALE_NORM=10;
    double G0_SCALE_MUT=5;
    double G1_SCALE_NORM=G0_SCALE_NORM;
    double G1_SCALE_MUT=G0_SCALE_MUT;
    double RAND_DEATH_PROB=0.0001;
    double DIV_RAD_PROP=2.0/3.0;
    double FORCE_CONST=1;
    double FORCE_EXP=2;
    GenomeTracker<CellGenome> normalGenomes;
    GenomeTracker<CellGenome> mutantGenomes;

    double[] divCoordScratch=new double[2];
    ArrayList<Cell> agentScratch=new ArrayList<>();

    public Tissue(int sideLen){
        super(sideLen,sideLen,Cell.class,true,true);
        this.rn=new Random();
    }
    public void Setup(int nNorm,int nMut){
        CellGenome normProg=new CellGenome(null,G0_SCALE_NORM,G1_SCALE_NORM,CELL_RAD_NORM,RAND_DEATH_PROB,CELL_CYCLE,false);
        this.normalGenomes=new GenomeTracker<>(normProg,true);
        for (int i = 0; i < nNorm; i++) {
            Cell c= NewAgentPT(rn.nextDouble()*xDim,rn.nextDouble()*yDim);
            c.Init(normProg);
            normProg.IncPop();
        }
        normProg.DecPop();
        CellGenome mutProg=new CellGenome(null,G0_SCALE_MUT,G1_SCALE_MUT,CELL_RAD_MUT,RAND_DEATH_PROB,CELL_CYCLE,true);
        this.mutantGenomes=new GenomeTracker<>(mutProg,true);
        for (int i = 0; i < nMut; i++) {
            Cell c= NewAgentPT(rn.nextDouble()*xDim,rn.nextDouble()*yDim);
            c.Init(mutProg);
            mutProg.IncPop();
        }
        mutProg.DecPop();
    }
    public void Step(){
        for (Cell c : this) {
            c.Observe();
        }
        for (Cell c : this) {
            c.Act();
        }
        CleanInc();
    }
}

class Cell extends SphericalAgent2D<Cell,Tissue> {
    int cycleCt;
    double forceSum;
    CellGenome Genome;
    void Init(CellGenome Genome){
        this.Genome=Genome;
        this.radius=Genome.startRad;
        this.cycleCt=0;
    }
    void Shrink(){
        if(this.cycleCt!=0) {
            this.cycleCt--;
            double nextRad = G().CELL_RAD_GROWTH_EXTRA * Genome.startRad * (1.0 / Genome.cellCycle) * this.cycleCt + Genome.startRad;
            this.radius=nextRad;
        }
    }
    void Grow(){
        this.cycleCt++;
        double nextRad=G().CELL_RAD_GROWTH_EXTRA*Genome.startRad*this.cycleCt*1.0/Genome.cellCycle+Genome.startRad;
        //this.radius=Math.max(this.radius,nextRad);
        this.radius=nextRad;
    }
    void Split(){
        Cell daugther=this.Divide(this.radius*G().DIV_RAD_PROP,G().divCoordScratch,G().rn);
        this.Init(Genome);
        //when cell divides, reset velocity
        this.xVel=0;
        this.yVel=0;
        daugther.Init(Genome);
        daugther.xVel=0;
        daugther.yVel=0;
        Genome.IncPop();
    }
    boolean CheckG0(double forceSum){
        double prob = Math.tanh(forceSum*Genome.g0scale);
        return G().rn.nextDouble()<prob;
    }
    boolean CheckG1(double forceSum){
        double prob = 1 - Math.tanh(forceSum*Genome.g1scale);
        return G().rn.nextDouble()<prob;
    }
    double OverlapForceCalc(double overlap){
        //computes the force to overlap function
        if(overlap>0){ return Math.abs(Math.pow(overlap*G().FORCE_CONST,G().FORCE_EXP)); }
            return 0;
    }
    void Observe(){
        //"Observe" step
        forceSum=SumForces((G().CELL_RAD_NORM*(1+G().CELL_RAD_GROWTH_EXTRA))*2,G().agentScratch,this::OverlapForceCalc);
        if(cycleCt==G().R_CYCLE_CHECK&&CheckG0(forceSum)) {
            cycleCt -= G().G0_BACKTRACK + G().rn.nextInt(G().G0_BACKTRACK+1);
        }
    }
    void Act(){
        //"Act" step
        if(G().rn.nextDouble()<Genome.randDeathProb){
            Genome.DecPop();
            Dispose();
            return;
        }
        ApplyFriction(G().FRICTION_CONST);
        ForceMove();
        Grow();
        if(cycleCt>=Genome.cellCycle){
            Split();
        }
    }
    boolean IsMut(){
        return Genome.isMut;
        //return startRad==G().CELL_RAD_MUT;
    }
}

