package Models.Raf.PrisonersModel;
import Framework.Extensions.PayoffMatrixGame;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

public class ChandlerGame extends PayoffMatrixGame{
    Random rn;
    TickTimer trt=new TickTimer();

    double MUTATION_PROB=0.01;
    double IMMUNOSUP_WALL_SCALER=0.0001;

    static final double BG=0.4;//BENEFIT_OF_GROWTH_FACTORS(bG)
    static final double BN=0.6;//BENEFIT_OF_TUMOR_WITH_NORMAL
    static final double CR=-2;//COST_OF_REACTIVE_OXYGEN_SPECIES(cR)
    static final double CP=-0.5;//COST OF ANY STRATEGY PHENOTYPE
    static final double CI=-1.5;//COST OF BEING SUCCEPTIBLE (IMMUNE ATTACK)

    //DEATH: choose random square of 9 cells and pick the lowest fitness agent, then swap with highest in moore hood around cell to be replaced
    //recalc fitnesses around swapped cell

    //EIRSN
    final static int EVASIVE=0;
    //does not trigger immune activity
    final static int IMMUNOSUPPRESS=1;
    //promote M2
    final static int RESISTANT=2;
    //promote M1, resistant to immune attack
    final static int SUCCEPTIBLE=3;
    final static int NORMAL=4;
    final static int EMPTY=5;
    final static int WALL=6;
    final static int N_TYPES=7;

    //walls degrade with probability related to fitness
    //empty cells have no fitness
    final static int[]colors=new int[]{
            RGB(0,0,1),
            RGB(0,1,0),
            RGB(1,0,0),
            RGB(0,1,1),
            RGB(0.5,0.5,0.5),
            RGB(0,0,0),
            RGB(1,0,1)
    };

    final static int[]payoffHood=VonNeumannHood(false);
    final static int[]replaceHood=VonNeumannHood(true);
    final static int[]wallCheckHood=MooreHood(false);
    int[]wallScratch=new int[wallCheckHood.length/2];
    final static double[] payoffMat=new double[]{
            CP,BG+CP,CP,CP,BN+CP,0,0,//evasive row
            BG+CP,2*BG+CP,BG+CP,BG+CP,BG+BN+CP,0,0,//immunosupressive row
            CP,BG+CP,CP,CP,BN+CP,0,0,//resistant row
            CI,BG+CI,CI,CI,BN+CI,0,0,//succeptible row
            0,BG,CR,CR,0,0,0,//normal row
            0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,

    };
    public void OffsetMat(double offset){
        for (int x = 0; x < nTypes-2; x++) {
            for (int y = 0; y < nTypes-2; y++) {
                payoffMat[x * nTypes + y] += offset;
            }
        }
    }
    public static ChandlerGame SetupGame(String initStatePath,double[]tumorTypes){
        SumTo1(tumorTypes);
        FileIO in=new FileIO(initStatePath,"r");
        ArrayList<int[]>data=new ArrayList<>();
        int nRows=0;
        int[] row=(in.ReadLineInts(","));
        int nCols=row.length;
        while(row!=null){
            data.add(row);
            nRows++;
            row=in.ReadLineInts(",");
        }
        System.out.println(nRows+","+nCols);
        ChandlerGame ret=new ChandlerGame(nCols,nRows,new Random());
        for (int x = 0; x < ret.xDim; x++) {
            for (int y = 0; y < ret.yDim; y++) {
                //System.out.println(data.get(y).length);
                //System.out.println(x+","+y);
                switch (data.get(y)[x]){
                    case 0:ret.SetType(x,y,NORMAL);break;
                    case 1:ret.SetType(x,y,EMPTY);break;
                    case 2:ret.SetType(x,y,RandomVariable(tumorTypes,ret.rn));break;
                    case 3:ret.SetType(x,y,WALL);break;
                    default:throw new IllegalArgumentException("input data contains invalid values");
                }
            }
        }
        return ret;
    }
    public ChandlerGame(int x,int y, Random rn){
        super(x,y,payoffMat,9,false,false,false);
        OffsetMat(2);
        //SetupType(RESISTANT);
        //SetupRandom(new double[]{0.9,0.09,0.01});
        this.rn=rn;
    }
    public void Step(long millis){
        trt.TickPause(millis);
        DefaultStep(rn);
        //Mutate();
    }
    public double CalcFitness(int i){
        int x=ItoX(i);
        int y=ItoY(i);
        double fitness = 0;
        int[] hood = GetFitnessHood(x, y);
        int nIs = HoodToIs(hood, localIs, x, y);
        int countOcc=nIs;
        for (int j = 0; j < nIs; j++) {
            if((int)GetType(localIs[j])<=NORMAL) {
                fitness += GetFitness(i, localIs[j]);
            }
            else{
                countOcc--;
            }
        }
        return fitness/countOcc;
    }
    public void ChangeState(int idTo, int idFrom) {
        int replaceType=(int)GetType(idTo);
        int fromType=(int)GetType(idFrom);
        if(replaceType!=WALL&&fromType!=WALL) {
            if (singleUpdate) {
                SetType(idTo, GetType(idFrom));
            } else {
                throw new IllegalStateException("Singleupdate has been diabled");
                //SetTypeSwap(idTo, GetType(idFrom));
            }
        }
        else if(!singleUpdate){
            throw new IllegalStateException("Singleupdate has been diabled");
            //SetTypeSwap(idTo,GetType(idTo));
        }
    }
    public int[] GetFitnessHood(int x, int y){ return payoffHood; }
    public int[] GetReplacementHood(int x, int y){ return replaceHood; }
    void Mutate(){
        for (int i = 0; i < length; i++) {
            int t=(int)GetType(i);
            if(t<=3&&rn.nextDouble()<MUTATION_PROB){
                SetType(i,rn.nextInt(4));
            }
            else if(t==WALL){
                int nIs= HoodToIs(wallCheckHood,wallScratch,ItoX(i),ItoY(i));
                int nImmunosup=0;
                for (int j = 0; j < nIs; j++) {
                    if((int)GetType(wallScratch[j])==IMMUNOSUPPRESS){
                        nImmunosup++;
                    }
                    if(rn.nextDouble()<(nImmunosup*IMMUNOSUP_WALL_SCALER)/nIs){
                        SetType(i,EMPTY);
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        GuiWindow win=new GuiWindow("disp",true);
        GuiGridVis vis=new GuiGridVis(613,868,1);
        win.AddCol(0, vis);
        win.RunGui();
        Random rn=new Random();
        ChandlerGame g=SetupGame("all_pos.csv",new double[]{1,1,1,1});
        FileIO payoffOut=new FileIO("payoffMat.csv","w");
        payoffOut.Write(g.GetPayoffMat(new String[]{ "EVASIVE","IMMUNOSUPPRESS","RESISTANT","SUCCEPTIBLE","NORMAL","EMPTY","WALL" }));
        payoffOut.Close();
        for (int i = 0; i < 100000; i++) {
            g.Mutate();
            g.Step(100);
            g.DrawTypes(vis,colors);
        }
        win.Dispose();
    }
}

