package Models.MelanomaWorkshop.Model3D;

import Framework.Extensions.SphericalAgent3D;
import Framework.GridsAndAgents.AgentGrid3D;
import Framework.Gui.Vis3DOpenGL;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.RGB;

class Dish extends AgentGrid3D<Cell> {
    final static int BLACK=RGB(0,0,0),RED=RGB(1,0,0),GREEN=RGB(0,1,0),YELLOW=RGB(1,1,0),BLUE=RGB(0,0,1);
    //GLOBAL CONSTANTS
    double DIVISION_PROB=0.01;
    double DEATH_PROB=0.0;
    //double FUSION_PROB=0.0;
    double FUSION_PROB=0.00002;
    double CELL_RAD=0.3;
    double MAX_RAD=Math.sqrt(2)*CELL_RAD;
    double FRICTION=0.9;
    double STEADY_STATE_FORCE=1000000000;
    double MAX_STEADY_STATE_LOOPS=2;
    double DIV_RADIUS=CELL_RAD*(2.0/3.0);
    double FORCE_EXPONENT=2;
    double FORCE_SCALER=0.7;
    double MAX_VEL=1000000000;

    int fusionCt=0;
    //INTERNAL VARIABLES
    Random rn=new Random();
    ArrayList<Cell> cellScratch=new ArrayList<>();
    double[] divCoordScratch=new double[3];

    public Dish(int sideLen,int startingPop,double startingRadius){
        super(sideLen,sideLen,sideLen, Cell.class);
        double[] startCoords=new double[3];
        for (int i = 0; i < startingPop; i++) {
            Utils.RandomPointInSphere(startingRadius, startCoords, rn);
            Cell c=NewAgentPT(startCoords[0]+xDim/2.0,startCoords[1]+yDim/2.0,startCoords[2]+zDim/2.0);
            if(i%2==0) {
                c.Init(RED);
            }
            else {
                c.Init(GREEN);
            }
        }
    }
    void Fusion(Cell c1, Cell c2){
        Cell H=NewAgentPT((c1.Xpt()+c2.Xpt())/2,(c1.Ypt()+c2.Ypt())/2,(c1.Zpt()+c2.Zpt())/2);
        if(c1.color==GREEN && c2.color==GREEN){
            H.Init(GREEN,true);
        }
        else if((c1.color==GREEN && c2.color==RED)||(c1.color==RED && c2.color==GREEN)){
            H.Init(YELLOW,true);
        }
        else if(c1.color==RED && c2.color==RED){
            H.Init(RED,true);
        }
        c1.Dispose();
        c2.Dispose();
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
        for (Cell c:this){
            fusionCt+=c.Fuse()?1:0;
        }
        //System.out.println(fusionCt);
        IncTick();
    }
}

class Cell extends SphericalAgent3D<Cell, Dish> {
    int color;
    boolean hybrid;

    void Init(int InitialColor){
        radius=G().CELL_RAD;
        xVel=0;
        yVel=0;
        zVel=0;
        color=InitialColor;
        hybrid=false;
    }
    void Init(int InitialColor,boolean IsHybrid){
        xVel=0;
        yVel=0;
        zVel=0;
        color=InitialColor;
        hybrid=IsHybrid;
        if(hybrid==false){
            radius=G().CELL_RAD;
        }
        else{
            radius=Math.sqrt(2)*G().CELL_RAD;
        }
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
    boolean Fuse(){
        if(hybrid){return false;}
        //listing all cells in the area
        G().cellScratch.clear();
        G().AgentsInRad(G().cellScratch,Xpt(),Ypt(),Zpt(),G().CELL_RAD*2);
        int neighborCt=0;
        //getting valid fusion neighbors
        for (int i=0;i<G().cellScratch.size();i++) {
            Cell c=G().cellScratch.get(i);
            if(!c.hybrid&&c!=this&&Utils.DistSquared(Xpt(),Ypt(),c.Xpt(),c.Ypt())<G().CELL_RAD*2){
                G().cellScratch.set(neighborCt,c);
                neighborCt++;
            }
        }
        //fusing
        if(neighborCt>0&&G().rn.nextDouble()<Utils.ProbScale(G().FUSION_PROB,neighborCt)){
            G().Fusion(this,G().cellScratch.get(G().rn.nextInt(neighborCt)));
            return true;
        }
        return false;
    }
    double Observe(){

        double ret = SumForces(radius+G().MAX_RAD,G().cellScratch,this::OverlapToForce);
        if(ret>G().MAX_VEL){
            xVel*=G().MAX_VEL/ret;
            yVel*=G().MAX_VEL/ret;
            zVel*=G().MAX_VEL/ret;
        }
        return ret;
    }
    void Act(){
        ForceMove();
        ApplyFriction(G().FRICTION);
    }
    void Step(){
        if(G().rn.nextDouble()<G().DEATH_PROB && hybrid==false){
            Dispose();
            return;
        }
        if(G().rn.nextDouble()<G().DIVISION_PROB && hybrid==false){
            Cell child=Divide(G().DIV_RADIUS,G().divCoordScratch,G().rn);
            child.Init(this.color);
            Init(this.color);
        }
    }
}

public class Model3D {
    static int SIDE_LEN=50;
    static int STARTING_POP=10;
    static double STARTING_RADIUS=1;
    static int TIMESTEPS=2000;
    static float[] circleCoords=Utils.GenCirclePoints(1,10);
    public static void main(String[] args) {
        //TickTimer trt=new TickRateTimer();
        Vis3DOpenGL vis=new Vis3DOpenGL("Cell Fusion Visualization", 1000,1000,SIDE_LEN,SIDE_LEN,SIDE_LEN);
        Dish d=new Dish(SIDE_LEN,STARTING_POP,STARTING_RADIUS);
        //d.SetCellsColor("red");
        for (int i = 0; i < TIMESTEPS&&!vis.CheckClosed(); i++) {
            vis.TickPause(0);
            d.Step();
            DrawCells(vis,d);
        }
    }

    static void DrawCells(Vis3DOpenGL vis, Dish d){
        vis.Clear(Dish.BLACK);
        for (Cell c:d) {
            //color "cytoplasm"
            //vis.Circle(c.Xpt(),c.Ypt(),c.Zpt(),c.radius,c.color);
            vis.CelSphere(c.Xpt(),c.Ypt(),c.Zpt(),c.radius,c.color);
        }
        vis.Show();
        //vis.ToPNG(path.concat(Integer.toString(i)));
    }
}
