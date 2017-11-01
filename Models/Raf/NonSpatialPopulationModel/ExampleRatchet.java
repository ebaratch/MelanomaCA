package Models.Raf.NonSpatialPopulationModel;

import Framework.Gui.GridVisWindow;
import Framework.Tools.NonSpatialPopModel;

import java.util.Random;
import static Framework.Utils.*;

/**
 * Created by Rafael on 10/16/2017.
 */
public class ExampleRatchet extends NonSpatialPopModel {
    //carrying CAPACITY (K)=10E7
    //init pop (T_init) =10E7*0.05
    //timestep (dt)=1.0/24 (in days)
    //birth rate (a)=log(2)/dt
    //ProbBirth=a(1-b/a)^m TODO check out why b/a is used
    //ProbDeath=T/K
    //ProbMutWithDrug=0.005
    //ProbMutWithoutDrug=0.5
    //Mut effect (b) (whatever maximizes the ratchet)
    //expt. duration: ?
    //N_Muts ? basically make inf.
    public final long capacity;
    public final double mutProb;
    public final double birthProbBase;
    public final double birthProbDec;

    public ExampleRatchet(long[] initPops,long capacity,double mutProb, Random rn,double birthProbBase,double birthProbDec) {
        super(initPops, rn);
        this.capacity = capacity;
        this.mutProb=mutProb;
        this.birthProbBase=birthProbBase;
        this.birthProbDec=birthProbDec;
        for (int i = 0; i < initPops.length-1; i++) {
            AddTransition(i,i+1,1);
        }
        Initialize();
    }

    @Override
    public double GetBirthProb(int iPop, long[] currPops, long totalPop) {
        //return BIRTH_PROB_BASE-iPop*BIRTH_PROB_DEC;
        return birthProbBase*Math.pow(1-birthProbDec/birthProbBase,iPop);
    }

    @Override
    public double GetDeathProb(int iPop, long[] currPops, long totalPop) {
        return totalPop*1.0/capacity;
    }

    @Override
    public double GetMutProb(int iPop, long[] currPops, long totalPop) {
        if(iPop<currPops.length-1) {
            return mutProb;
        }
        return 0;
    }

    public static void main(String[] args) {
        int[]colors=new int[11];
        colors[0]=RGB(0,0,1);
        for (int i = 1; i < colors.length; i++) {
            colors[i]=HeatMapRGB(i,-2,10);
        }

        ExampleRatchet ex=new ExampleRatchet(new long[]{500000,0,0,0,0,0,0,0,0,0}, 10000000,0.1,new Random(),0.5,0.04);
        GridVisWindow win=new GridVisWindow("testWin",500,300,2);
        for (int i = 0; i < 100000; i++) {
            win.TickPause(0);
            ex.Step();
            if(i%100000/500==0) {
                ex.DrawPops(win, colors, 10000000);
            }
        }
    }
}
