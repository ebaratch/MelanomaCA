package Models.JeffreyModels.CompetitiveRelease;

import Framework.Extensions.ClinicianSim;
import Framework.Extensions.PayoffMatrixGame;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Tools.Binomial;
import Framework.Utils;
import Framework.Interfaces.TreatableTumor;

import java.io.Serializable;
import java.util.Random;

import static Framework.Utils.CircleHood;
import static Framework.Utils.HSBColor;

public class CompRel extends PayoffMatrixGame implements TreatableTumor,Serializable{
    final static int NORMAL=0,SENSITIVE=1,RESISTANT=2;
    final int[]fithood= Utils.MooreHood(true);
    final int[]rephood= Utils.MooreHood(true);
    final static  double BOUND_VAL = 10;
    Random rn=new Random();

    // circle hoods
    private int[]largeCircleHood=CircleHood(true, 20);
    private int[]largeCircleArrayIs=new int[largeCircleHood.length/2];

    // smaller circle
    private int[]smallCircleHood=CircleHood(true, 3);
    private int[]smallCircleArrayIs=new int[smallCircleHood.length/2];

    //Random rn=new Random();
    int[]typeColors=new int[]{Utils.RGB(0,0,1),Utils.RGB(1,0,0),Utils.RGB(0,1,0)};
    PDEGrid2D drug;
    double w=1.0;
    int TOTAL_CELLS;
    //double CMAX = 2.0;

    public CompRel(int x, int y, double[] payoffs, int maxHoodSize, boolean singleUpdate, boolean wrapX, boolean wrapY) {
        super(x, y, payoffs, maxHoodSize, singleUpdate, wrapX, wrapY);
        drug=new PDEGrid2D(x,y);
        TOTAL_CELLS = x * y;
    }

    @Override
    public double GetFitness(int idTo, int idOther) {
        double thisW=ComputeConcFit(idTo);
        return (1-thisW)+thisW*super.GetFitness(idTo, idOther);
    }
    public double ComputeConcFit(int i){
        int type=GetType(i);
        double c=drug.Get(i);
        //c = const_c;
        switch (type){
            case 0:return w;//(1+c)*w;
            case 1:return (1-c)*w;
            case 2:return 1*w;
            default:throw new IllegalStateException("type not in 0-2 range: "+type);
        }
    }

    @Override
    public int[] GetFitnessHood(int x, int y) {
        return fithood;
    }

    @Override
    public int[] GetReplacementHood(int x, int y) {
        return rephood;
    }
    public static double[] MakePayoffMatrix(double p1,double p2,double p3, double base) {

        double[] payoff = new double[]{
                base,base,base,
                base+p1,base,base,
                base+p2,base-p3,base
        };

        return payoff;
    }
    public void Step(double boundVal,Random rn, double death_rate,int step){


        if (step % 10 == 0) {
            for (int i = 0; i < 50; i++) {
                drug.Diffusion(0.25, boundVal);
            }
        }


//
        for (int i = 0; i < length; i++) {
            if(GetType(i)!=NORMAL){
                drug.Mul(i,0.99);
            }
        }
//        DefaultStep(rn);

//        drug.Diffusion(0.25, boundVal);

        // pick binomial ( N cells, death_rate) for death, kill least fit neighbor
        Binomial bn=new Binomial();
        int res=bn.SampleInt(TOTAL_CELLS,death_rate,rn);

        for (int i = 0; i < length; i++) {
            if (i >= res) { break; }
            DefaultStep(rn);
        }
        //IncTick();

    }

    public void Init(){
        for (int i = 0; i < length; i++) {
            SetType(i,NORMAL);
        }

        // add in 0th type (LARGE)
        int largerPop = HoodToIs(largeCircleHood,largeCircleArrayIs,xDim/2,yDim/2);
        for (int j = 0; j < largerPop; j++) {
            SetType(largeCircleArrayIs[j],SENSITIVE);
        }

        // add in 0th type (SMALL)
        int smallerPop = HoodToIs(smallCircleHood,smallCircleArrayIs,xDim/2,yDim/2);
        for (int j = 0; j < smallerPop; j++) {
            SetType(smallCircleArrayIs[j],RESISTANT);
        }
        System.out.println("larger : " + (largerPop - smallerPop) + " smaller : " + smallerPop );
    }
    public static void Run(){
        // death
        //static double death_rate = 0.1;

        CompRel model=new CompRel(100,100,MakePayoffMatrix(0.2,0.2,0.7,25.0),9,false,true,true);
        model.Init();
        GuiWindow win=new GuiWindow("CompRel",true);
        GuiGridVis visTypes = new GuiGridVis(100,100,5);
        GuiGridVis visDrug = new GuiGridVis(100,100,5);
        win.AddCol(0,visTypes);
        win.AddCol(1,visDrug);
        win.RunGui();
        Random rn =new Random();
        for (int i = 0; i < 1000; i++) {
            win.TickPause(50);
            model.DrawTypes(visTypes,model.typeColors);
            visDrug.DrawGridDiff(model.drug,(val)->{
                return Utils.HeatMapRBG(Utils.RescaleMinToMax(val, (double) 0, BOUND_VAL));
            });
            model.Step(BOUND_VAL,rn,0.1,i);
        }
        win.Dispose();
    }
    public static void main(String[] args) {
        CompRel model=new CompRel(100,100,MakePayoffMatrix(2,2,0,2.0),9,true,true,true);
        model.Init();
        ClinicianSim ccl=new ClinicianSim(model,1000,10,5,1,20,20,5,100);
        ccl.RunGui();
        ccl.RunModel();
    }

    @Override
    public void Draw(GuiGridVis vis, GuiGridVis alphaVis, boolean[] switchVals) {
        DrawTypes(vis,typeColors); for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                vis.SetPix(x+xDim, y,Utils.HeatMapRGB(drug.Get(x,y)));
            }
        }
    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {
        Step(treatmentVals[0]*BOUND_VAL,rn,0.1,step);
    }

    @Override
    public String[] GetTreatmentNames() {
        return new String[]{"Chemo"};
    }

    @Override
    public int[] GetTreatmentColors() {
        return new int[]{HSBColor(0,1,1)};
    }

    @Override
    public int GetNumIntensities() {
        return 5;
    }

    @Override
    public void SetupConstructors() {
        return;
    }

    @Override
    public int VisPixX() {
        return xDim*2;
    }

    @Override
    public int VisPixY() {
        return yDim;
    }

    @Override
    public double GetTox() {
        return drug.GetAvg();
    }

    @Override
    public double GetBurden() {
        int ct=0;
        for (int i = 0; i < length; i++) {
            if(GetType(i)!=NORMAL){
                ct++;
            }
        }
        return ct*1.0/length;
    }

    @Override
    public double GetMaxTox() {
        return 0;
    }

    @Override
    public double GetMaxBurden() {
        return 0;
    }

//    @Override
//    public String[] GetSwitchNames() {
//        return new String[0];
//    }
//
//    @Override
//    public boolean AllowMultiswitch() {
//        return false;
//    }
}
