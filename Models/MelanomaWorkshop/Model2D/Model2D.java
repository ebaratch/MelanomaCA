package Models.MelanomaWorkshop.Model2D;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.Vis2DOpenGL;
import Framework.Utils;
import sun.jvm.hotspot.oops.CellTypeState;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

class Dish extends AgentGrid2D<Cell> {
    final static int BLACK=RGB(0,0,0),RED=RGB(1,0,0),GREEN=RGB(0,1,0),YELLOW=RGB(1,1,0),BLUE=RGB(0,0,1);
    //GLOBAL CONSTANTS
    double DIVISION_PROB=0.01;
    double DEATH_PROB=0.0;
    double CELL_RAD=0.3;
    double MAX_RAD=Math.sqrt(2)*CELL_RAD;
    double FRICTION=0.9;
    double STEADY_STATE_FORCE=0;
    double MAX_STEADY_STATE_LOOPS=10;
    double DIV_RADIUS=CELL_RAD*(2.0/3.0);
    double FORCE_EXPONENT=2;
    double FORCE_SCALER=0.7;
    //double MAX_VEL=1000000000;

    //INTERNAL VARIABLES
    Random rn=new Random();
    ArrayList<Cell> cellScratch=new ArrayList<>();
    double[] divCoordScratch=new double[2];

    public Dish(int sideLen,int startingPop,double startingRadius){
        super(sideLen,sideLen,Cell.class);
        double[] startCoords=new double[2];
        for (int i = 0; i < startingPop; i++) {
            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
            Cell c=NewAgentPT(startCoords[0]+xDim/2.0,startCoords[1]+yDim/2.0);
            c.Init(RED);
        }
    }


    int SteadyStateMovement(){
        int loopCt=0;
        while(loopCt<MAX_STEADY_STATE_LOOPS) {
            double maxForce=0;
            for (Cell c : this) {
                maxForce=Math.max(maxForce,c.Observe());
            }
            for (Cell c : this) {
                c.Act();
            }
            loopCt++;
            if(maxForce<STEADY_STATE_FORCE){
                //System.out.println(loopCt+","+maxForce);
                break;
            }
        }
        return loopCt;
    }
    void Step(){
        SteadyStateMovement();
        //for (Cell c:this) {
        //    c.Observe();
        //}
        //for (Cell c:this){
        //    c.Act();
        //}
        for (Cell c:this) {
            c.Step();
        }
        //System.out.println(fusionCt);
        IncTick();
    }
}

class Cell extends SphericalAgent2D<Cell,Dish> {
    int color=G().RED;
    String Type;


    void Init(int InitialColor){
        radius=G().CELL_RAD;
        xVel=0;
        yVel=0;
        color=InitialColor;
    }

    void SetCellColor(int newColor){
        color=newColor;
    }

    double OverlapToForce(double overlap){
        if(overlap<0){
            return 0;
        }
        return Math.pow(G().FORCE_SCALER*overlap,G().FORCE_EXPONENT);
        //return G().FORCE_SCALER*overlap;
    }

    double Observe(){

        return SumForces(radius+G().MAX_RAD,G().cellScratch,this::OverlapToForce);
        /*
        if(ret>G().MAX_VEL){
            xVel*=G().MAX_VEL/ret;
            yVel*=G().MAX_VEL/ret;
        }
        return ret;
        */
    }
    void Act(){
        ForceMove();
        ApplyFriction(G().FRICTION);
    }
    void Step(){
        if(G().rn.nextDouble()<G().DEATH_PROB){
            Dispose();
            return;
        }
        if(G().rn.nextDouble()<G().DIVISION_PROB){
            Cell child=Divide(G().DIV_RADIUS,G().divCoordScratch,G().rn);
            child.Init(this.color);
            Init(this.color);
        }
    }
}


public class Model2D {
    static int SIDE_LEN=70;
    static int STARTING_POP=1;
    static double STARTING_RADIUS=10;
    static int TIMESTEPS=2000;
    static float[] circleCoords=Utils.GenCirclePoints(1,10);
    public static void main(String[] args) {
        //TickTimer trt=new TickRateTimer();
        Vis2DOpenGL vis=new Vis2DOpenGL("Cell Fusion Visualization", 1000,1000,SIDE_LEN,SIDE_LEN);
        Dish d=new Dish(SIDE_LEN,STARTING_POP,STARTING_RADIUS);
        //d.SetCellsColor("red");
        for (int i = 0; i < TIMESTEPS; i++) {
            vis.TickPause(0);
            d.Step();
            DrawCells(vis,d);
        }
    }

    static void DrawCells(Vis2DOpenGL vis,Dish d){
        vis.Clear(Dish.BLACK);
        for (Cell c:d) {
            //color "cytoplasm"
            vis.Circle(c.Xpt(),c.Ypt(),c.radius,c.color);
        }
        for (Cell c:d) {
            //color "nucleus"
            vis.Circle(c.Xpt(),c.Ypt(),c.radius/2.0, Dish.BLUE);
        }
        vis.Show();
        //vis.ToPNG(path.concat(Integer.toString(i)));
    }
}
