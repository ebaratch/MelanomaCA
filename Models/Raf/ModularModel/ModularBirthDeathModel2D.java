package Models.Raf.ModularModel;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;

import java.util.ArrayList;
import java.util.Random;

public abstract class ModularBirthDeathModel2D<A extends ModularBirthDeathCell2D> extends AgentGrid2D<A> {
    //movement rule options
    public static int NO_MOVEMENT=0,ONLY_INTO_EMPTY_MOVEMENT=1,PREFER_EMPTY_MOVEMENT=2,RANDOM_MOVEMENT=3;
    //division rule options
    public static int ONLY_INTO_EMPTY_DIVIDE=0,PREFER_EMPTY_DIVIDE=1,RANDOM_DIVIDE=2;
    int nProps=0;
    int nDiffs=0;
    int nModules=0;
    double divProbFactorSum=0;
    double dieProbFactorSum=0;
    double moveProbFactorSum=0;
    PDEGrid2D[]diffs;
    boolean initialized=false;
    final int movementRule;
    final int divRule;
    int[]neighborhoodIs;
    int[]emptyIs;
    int[]occupiedIs;
    int nOccupied;
    int nEmpty;
    int nNeighborhoodIs;
    final ArrayList<BirthDeathModule> modules=new ArrayList<>();
    public ModularBirthDeathModel2D(int x, int y,int movementRule,int divRule,boolean wrapX,boolean wrapY,int maxHoodLength,Class<A> agentClass){
        super(x, y, agentClass, wrapX, wrapY);
        this.movementRule=movementRule;
        this.divRule=divRule;
        this.neighborhoodIs=new int[maxHoodLength];
        this.emptyIs=new int[maxHoodLength];
        this.occupiedIs=new int[maxHoodLength];
        this.wrapX=wrapX;
        this.wrapY=wrapY;
    }
    @Override
    public A NewAgentSQ(int i){
        A cell=super.NewAgentSQ(i);
        cell.SetupProps();
        return cell;
    }
    @Override
    public A NewAgentSQ(int x,int y){
        A cell=super.NewAgentSQ(x,y);
        cell.SetupProps();
        return cell;
    }


    public void Init(){
        if(initialized){
            throw new IllegalStateException("can't initialize model twice");
        }
        initialized=true;
        diffs=new PDEGrid2D[nDiffs];
        nModules=modules.size();
        for (BirthDeathModule m : modules) {
            divProbFactorSum+=m.divProbFactor;
            dieProbFactorSum+=m.dieProbFactor;
            moveProbFactorSum+=m.moveProbFactor;
        }
    }
    public void StepModules(Random rn){
        for (BirthDeathModule module : modules) {
            module.StepDiff();
        }
        for (ModularBirthDeathCell2D cell : this) {
                cell.Step(rn);
        }
    }
    public abstract int[] _GetNeighborhood(int x, int y);
    public abstract void _RecordDeath(int reason, A dead);
    public abstract void _RecordMove(int reason, int prevPosI, A mover, A swappedCell);
    public abstract void _RecordDivide(int reason, A parent, A child, A killedCell);
}
