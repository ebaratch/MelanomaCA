

package Models.MelanomaWorkshop.Model2D;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.GridBase;
import Framework.Gui.Vis2DOpenGL;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

// txt reading/writing
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;


class Dish extends AgentGrid2D<Cell> {
    final static int BLACK=RGB(0,0,0),RED=RGB(1,0,0),GREEN=RGB(0,1,0),YELLOW=RGB(1,1,0),BLUE=RGB(0,0,1), AZUL=RGB(1,0,1);
    //GLOBAL CONSTANTS
    double DIVISION_PROB=0.01;
    double DEATH_PROB=0.0;
    double[] CELL_RAD={0.5,0.3,0.3,0.3};
    double[] MAX_RAD=new double[CELL_RAD.length];
    double FRICTION=0.9;
    double STEADY_STATE_FORCE=0;
    double MAX_STEADY_STATE_LOOPS=10;
    double[] DIV_RADIUS=new double[CELL_RAD.length];
    double FORCE_EXPONENT=2;
    double FORCE_SCALER=0.7;
    double immAttr; // alpha
    double stromAttr; // beta
    double[] divProb={0.007,0.0,0.001,0.01};
    double[] deathProb={0.0,0.0,0.0,0.0};

    //double MAX_VEL=1000000000;

    int fusionCt=0;
    //INTERNAL VARIABLES
    Random rn=new Random();
    ArrayList<Cell> cellScratch=new ArrayList<>();
    ArrayList<Cell> cancerCells=new ArrayList<>();
    ArrayList<Cell> stromaCells=new ArrayList<>();
    ArrayList<Cell> immuneCells=new ArrayList<>();
    ArrayList<double[]>BloodVesselsCoord=new ArrayList<>();
    double[] Ves1={xDim/4,yDim/4};
    double[] Ves2={xDim*3/4,yDim*3/4};
    double[] Ves3={xDim/4,yDim*3/4};
    double[] Ves4={xDim*3/4,yDim/4};


    public List<String> initImageFile(String path_to_file){
        List<String> lines = new ArrayList<String>();
        try {
            lines = Files.readAllLines(Paths.get(path_to_file));
            //for (String line : lines) {
                //System.out.println(line);
            //}
        }
        catch(IOException e) {
            e.printStackTrace();

        }
        return lines;
    }

    public void writeToFile(List<String> array_list){

    }


    double[] divCoordScratch=new double[2];


    public Dish(int sideLen,int startingPop,int startingStroma,double startingRadius, String path_to_file){
        super(sideLen,sideLen,Cell.class);
        double[] startCoords=new double[2];
        for (int i = 0; i < CELL_RAD.length ; i++) {
            MAX_RAD[i]=Math.sqrt(2)*CELL_RAD[i];
            DIV_RADIUS[i]=CELL_RAD[i]*(2.0/3.0);
        }
        BloodVesselsCoord.add(Ves1);
        BloodVesselsCoord.add(Ves2);
        BloodVesselsCoord.add(Ves3);
        BloodVesselsCoord.add(Ves4);

        double[] startCoordsStroma={10,10};


        if (path_to_file == null) {
            for (int i = 0; i < startingStroma; i++) {
                //            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
                Cell c = NewAgentPT(Math.round(Math.random() * xDim), Math.round(Math.random() * yDim));
                c.Init(1);
                stromaCells.add(c);
            }

            for (int i = 0; i < startingPop; i++) {
                //            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
                Cell c = NewAgentPT(xDim / 2.0, yDim / 2.0);
                //            Cell c=NewAgentPT(startCoords[0]+xDim/2.0,startCoords[1]+yDim/2.0);
                c.Init(0);
                cancerCells.add(c);
            }
        }
        else{
            // Get cells from cell list
            List<String> cell_list = initImageFile(path_to_file);
            // create seeds
            for (int i = 2; i < cell_list.size(); i++) {
            //for (int i = 950; i < 960; i++) {
                String line = cell_list.get(i);
                String[] line_array = line.split(",");
                
                double x = Double.parseDouble(line_array[2]);
                double y = Double.parseDouble(line_array[3]);
                int type = Integer.parseInt(line_array[1]) - 1;

                if (x < xDim && y < yDim && type < 3){
                    System.out.println("----- seeding");
                    System.out.println(x);
                    System.out.println(y);
                    System.out.println(type);

                    Cell c=NewAgentPT(x*xDim,y*yDim);
                    c.Init(type);
                }

            }
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
    void immuneArrival(){
        int tick= GetTick();
        if ((tick % 50)==1){
            for (int i = 0; i <BloodVesselsCoord.size() ; i++) {
            Cell c=NewAgentPT(BloodVesselsCoord.get(i)[0],BloodVesselsCoord.get(i)[1]);
            c.Init(1); //immune
            immuneCells.add(c);
            }
        }
    }

    void Step(){

        //CHEMICALS
        //update concentrations

        //CELLS
        //division
        //death




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
        immuneArrival();
        IncTick();
    }
}

class Cell extends SphericalAgent2D<Cell,Dish> {
    int color;
    int type;
    static double[] motility={0.01,0.01,0.1,0.1};
    double xVelStart;
    double yVelStart;
    double deviation;
    double maxVelAbs;


    void Init(int tType){
        radius=G().CELL_RAD[tType];
        type=tType;
        maxVelAbs=motility[type];
        xVelStart=motility[type]*Math.random()-motility[type]/2;
        yVelStart=motility[type]*Math.random()-motility[type]/2;
        deviation=motility[type]/2;
        xVel=0;
        yVel=0;
        SetCellColor();
    }

    void SetCellColor(){

        switch (type) {
            case 0:  color = G().RED;
                break;
            case 1:  color = G().AZUL;
                break;
            case 2:  color = G().YELLOW;
                break;
            case 3:  color = G().YELLOW;
                break;
        }
    }

    double OverlapToForce(double overlap){
        if(overlap<0){
            return 0;
        }
        return Math.pow(G().FORCE_SCALER*overlap,G().FORCE_EXPONENT);
        //return G().FORCE_SCALER*overlap;
    }

    double Observe(){

        return SumForces(radius+G().MAX_RAD[type],G().cellScratch,this::OverlapToForce);
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
        if(G().rn.nextDouble()<G().deathProb[type]){
            Dispose();
            return;
        }
        if(G().rn.nextDouble()<G().divProb[type]){
            Cell child=Divide(G().DIV_RADIUS[type],G().divCoordScratch,G().rn);
            child.Init(this.type);
            Init(this.type);
        }

        moveCell();

    }
    void moveCell(){
        switch (type) {
            case 0:  // cancer cells do not move
                break;
            case 1:
                xVelStart=xVelStart-(deviation/2)+deviation*Math.random();
                yVelStart=yVelStart-(deviation/2)+deviation*Math.random();
                this.xVel=xVelStart-(motility[type]/2)+motility[type]*Math.random();
                this.yVel=yVelStart-(motility[type]/2)+motility[type]*Math.random();
                limitVel();
//                double ptX=this.Xpt();
//                double ptY=this.Ypt();


//                this.MoveSafePT(ptX-(motility[type]/2)+motility[type]*Math.random(),ptY-(motility[type]/2)+motility[type]*Math.random());
                break;
            case 2:  color = G().YELLOW;
                break;
            case 3:  color = G().YELLOW;
                break;
        }

    }
    void limitVel(){
        if (this.xVel<-maxVelAbs){
            this.xVel=-maxVelAbs;
        }
        else if (this.xVel>maxVelAbs){
            this.xVel=maxVelAbs;
        }

        if (this.yVel<-maxVelAbs){
            this.yVel=-maxVelAbs;
        }
        else if (this.yVel>maxVelAbs){
            this.yVel=maxVelAbs;
        }
    }

}

public class Model2D {
    static int SIDE_LEN=70;
    static int STARTING_POP=1;
    static int STARTING_STROMA=17;
    static double STARTING_RADIUS=20;
    static int TIMESTEPS=4000;


    static float[] circleCoords=Utils.GenCirclePoints(1,10);
    public static void main(String[] args) {
         double[] motility={1,1,1,1};

        //TickTimer trt=new TickRateTimer();
        Vis2DOpenGL vis=new Vis2DOpenGL("Cell Fusion Visualization", 1000,1000,SIDE_LEN,SIDE_LEN);


        String path_to_file = "/Users/dabler/Documents/spatialstats/testdata.csv";
        //List<String> list = d.initImageFile(path_to_file);

        Dish d=new Dish(SIDE_LEN,STARTING_POP,STARTING_STROMA, STARTING_RADIUS, path_to_file);
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
