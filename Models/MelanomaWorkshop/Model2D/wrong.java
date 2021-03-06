//
//
//
//package Models.MelanomaWorkshop_Niccolo.Model2D;
//
//        import Framework.Extensions.SphericalAgent2D;
//        import Framework.GridsAndAgents.AgentGrid2D;
//        import Framework.Gui.Vis2DOpenGL;
//        import Framework.Utils;
//
//        import java.util.ArrayList;
//        import java.util.Random;
//
//        import static Framework.Utils.*;
//
//class Dish extends AgentGrid2D<Models.MelanomaWorkshop_Niccolo.Model2D.Cell> {
//    final static int BLACK=RGB(0,0,0),RED=RGB(1,0,0),GREEN=RGB(0,1,0),YELLOW=RGB(1,1,0),BLUE=RGB(0,0,1);
//    //GLOBAL CONSTANTS
//    double DIVISION_PROB=0.01;
//    double DEATH_PROB=0.0;
//    double[] CELL_RAD={0.3,0.3,0.3,0.3};
//    double[] MAX_RAD=new double[CELL_RAD.length];
//    double FRICTION=0.9;
//    double STEADY_STATE_FORCE=0;
//    double MAX_STEADY_STATE_LOOPS=10;
//    double[] DIV_RADIUS=new double[CELL_RAD.length];
//    double FORCE_EXPONENT=2;
//    double FORCE_SCALER=0.7;
//    double immAttr; // alpha
//    double stromAttr; // beta
//    double[] divProb={0.01,0.01,0.01,0.01};
//    double[] deathProb={0.0,0.0,0.0,0.0};
//    double[] motility={0,3,1,1};
//
//    //double MAX_VEL=1000000000;
//
//    int fusionCt=0;
//    //INTERNAL VARIABLES
//    Random rn=new Random();
//    ArrayList<Models.MelanomaWorkshop_Niccolo.Model2D.Cell> cellScratch=new ArrayList<>();
//    ArrayList<Models.MelanomaWorkshop_Niccolo.Model2D.Cell> cancerCells=new ArrayList<>();
//    ArrayList<Models.MelanomaWorkshop_Niccolo.Model2D.Cell> stromaCells=new ArrayList<>();
//    ArrayList<Models.MelanomaWorkshop_Niccolo.Model2D.Cell> immuneCells=new ArrayList<>();
//
//
//    double[] divCoordScratch=new double[2];
//
//    public Dish(int sideLen,int startingPop,int startingStroma,double startingRadius){
//        super(sideLen,sideLen, Models.MelanomaWorkshop_Niccolo.Model2D.Cell.class);
//        double[] startCoords=new double[2];
//        for (int i = 0; i < CELL_RAD.length ; i++) {
//            MAX_RAD[i]=Math.sqrt(2)*CELL_RAD[i];
//            DIV_RADIUS[i]=CELL_RAD[i]*(2.0/3.0);
//        }
//        for (int i = 0; i < startingPop; i++) {
//            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
//            Models.MelanomaWorkshop_Niccolo.Model2D.Cell c=NewAgentPT(startCoords[0]+xDim/2.0,startCoords[1]+yDim/2.0);
//            c.Init(0);
//            cancerCells.add(c);
//        }
//
//        double[] startCoordsStroma={10,10};
//        for (int i = 0; i < startingStroma; i++) {
////            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
//            Models.MelanomaWorkshop_Niccolo.Model2D.Cell c=NewAgentPT(Math.round(Math.random()*xDim),Math.round(Math.random()*yDim));
//            c.Init(1);
//            stromaCells.add(c);
//        }
//    }
//
//    int SteadyStateMovement(){
//        int loopCt=0;
//        while(loopCt<MAX_STEADY_STATE_LOOPS) {
//            double maxForce=0;
//            for (Models.MelanomaWorkshop_Niccolo.Model2D.Cell c : this) {
//                maxForce=Math.max(maxForce,c.Observe());
//            }
//            for (Models.MelanomaWorkshop_Niccolo.Model2D.Cell c : this) {
//                c.Act();
//            }
//            loopCt++;
//            if(maxForce<STEADY_STATE_FORCE){
//                //System.out.println(loopCt+","+maxForce);
//                break;
//            }
//        }
//        return loopCt;
//    }
//    void Step(){
//
//        //CHEMICALS
//        //update concentrations
//
//        //CELLS
//        //division
//        //death
//        //move
//
//
//        SteadyStateMovement();
//        //for (Cell c:this) {
//        //    c.Observe();
//        //}
//        //for (Cell c:this){
//        //    c.Act();
//        //}
//        for (Models.MelanomaWorkshop_Niccolo.Model2D.Cell c:this) {
//            c.Step();
//        }
//        //System.out.println(fusionCt);
//        IncTick();
//    }
//}
//
//class Cell extends SphericalAgent2D<Models.MelanomaWorkshop_Niccolo.Model2D.Cell,Models.MelanomaWorkshop_Niccolo.Model2D.Dish> {
//    int color;
//    int type;
//
//
//
//    void Init(int tType){
//        radius=G().CELL_RAD[tType];
//        type=tType;
//        xVel=0;
//        yVel=0;
//        SetCellColor();
//    }
//
//    void SetCellColor(){
//
//        switch (type) {
//            case 0:  color = G().RED;
//                break;
//            case 1:  color = G().BLUE;
//                break;
//            case 2:  color = G().GREEN;
//                break;
//            case 3:  color = G().YELLOW;
//                break;
//        }
//    }
//
//    double OverlapToForce(double overlap){
//        if(overlap<0){
//            return 0;
//        }
//        return Math.pow(G().FORCE_SCALER*overlap,G().FORCE_EXPONENT);
//        //return G().FORCE_SCALER*overlap;
//    }
//
//    double Observe(){
//
//        return SumForces(radius+G().MAX_RAD[type],G().cellScratch,this::OverlapToForce);
//        /*
//        if(ret>G().MAX_VEL){
//            xVel*=G().MAX_VEL/ret;
//            yVel*=G().MAX_VEL/ret;
//        }
//        return ret;
//        */
//    }
//    void Act(){
//        ForceMove();
//        ApplyFriction(G().FRICTION);
//    }
//    void Step(){
//        if(G().rn.nextDouble()<G().deathProb[type]){
//            Dispose();
//            return;
//        }
//        if(G().rn.nextDouble()<G().divProb[type]){
//            Models.MelanomaWorkshop_Niccolo.Model2D.Cell child=Divide(G().DIV_RADIUS[type],G().divCoordScratch,G().rn);
//            child.Init(this.color);
//            Init(this.color);
//        }
//    }
//}
//
//public class Model2D {
//    static int SIDE_LEN=70;
//    static int STARTING_POP=6;
//    static int STARTING_STROMA=19;
//    static double STARTING_RADIUS=10;
//    static int TIMESTEPS=4000;
//    static float[] circleCoords=Utils.GenCirclePoints(1,10);
//    public static void main(String[] args) {
//        //TickTimer trt=new TickRateTimer();
//        Vis2DOpenGL vis=new Vis2DOpenGL("Cell Fusion Visualization", 1000,1000,SIDE_LEN,SIDE_LEN);
//        Models.MelanomaWorkshop_Niccolo.Model2D.Dish d=new Models.MelanomaWorkshop_Niccolo.Model2D.Dish(SIDE_LEN,STARTING_POP,STARTING_STROMA, STARTING_RADIUS);
//        //d.SetCellsColor("red");
//        for (int i = 0; i < TIMESTEPS; i++) {
//            vis.TickPause(0);
//            d.Step();
//            DrawCells(vis,d);
//        }
//    }
//
//    static void DrawCells(Vis2DOpenGL vis, Models.MelanomaWorkshop_Niccolo.Model2D.Dish d){
//        vis.Clear(Models.MelanomaWorkshop_Niccolo.Model2D.Dish.BLACK);
//        for (Models.MelanomaWorkshop_Niccolo.Model2D.Cell c:d) {
//            //color "cytoplasm"
//            vis.Circle(c.Xpt(),c.Ypt(),c.radius,c.color);
//        }
//        for (Models.MelanomaWorkshop_Niccolo.Model2D.Cell c:d) {
//            //color "nucleus"
//            vis.Circle(c.Xpt(),c.Ypt(),c.radius/2.0, Models.MelanomaWorkshop_Niccolo.Model2D.Dish.BLUE);
//        }
//        vis.Show();
//        //vis.ToPNG(path.concat(Integer.toString(i)));
//    }
//}
