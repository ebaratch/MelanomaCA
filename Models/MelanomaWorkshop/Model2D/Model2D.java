

package Models.MelanomaWorkshop.Model2D;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.GridBase;
import Framework.GridsAndAgents.PDEGrid2D;

import Framework.Gui.Vis2DOpenGL;
import Framework.Utils;
import com.sun.xml.internal.xsom.impl.scd.Iterators;
import org.lwjgl.Sys;
import Framework.Gui.GridVisWindow;
import Framework.Gui.Vis2DOpenGL;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

// txt reading/writing
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import java.io.File;


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
    double[] divProb={0.001,0.0,0.0,0.01};
    double[] deathProb={0.0,0.0,0.0,0.0};
    double baseCaDeathProb=deathProb[0];
    double productionRate=0.1;
    double decayRate=0.1;
    int vessels_grid_x = 4;
    int vessels_grid_y = 4;
    PDEGrid2D diffusible = new PDEGrid2D(xDim, yDim); //Diffusible factor (VEGF,FGF,TGFB)

    //double MAX_VEL=1000000000;

    int fusionCt=0;
    //INTERNAL VARIABLES
    Random rn=new Random();
    ArrayList<Cell> cellScratch=new ArrayList<>();
    ArrayList<Cell> cancerCells=new ArrayList<>();
    ArrayList<Cell> stromaCells=new ArrayList<>();
    ArrayList<Cell> immuneCells=new ArrayList<>();
    ArrayList<double[]>BloodVesselsCoord=new ArrayList<>();

    public ArrayList<double[]> addBloodVessels(int n_vessels_x, int n_vessels_y){
        ArrayList<double[]>BloodVesselsCoord=new ArrayList<>();
        double dx = xDim / (n_vessels_x+1);
        double dy = yDim / (n_vessels_y+1);
        double x=dx;
        double y=dy;
        for (int i = 0; i < n_vessels_x; i++) {
            for (int j = 0; j < n_vessels_y; j++) {
                double[] Ves = {dx + dx*i, dy+dy*j};
                BloodVesselsCoord.add(Ves);
            }
        }
        return BloodVesselsCoord;

    }



    public List<String> readCellCoords(String path_to_file){
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


    public void writeCellCoords(String path_to_file){
        try
        {
            File file = new File(path_to_file);
            PrintWriter printWriter = new PrintWriter(file);
            String line = "";
            line = "cell_id,type,x,y\n";
            printWriter.print(line);
            int cnt = 0;
            for (Cell c:this) {

                int type = c.type;
                double x = c.Xpt();
                double y = c.Ypt();
                System.out.println(x);
                System.out.println(y);
                System.out.println(type);
                line = "" + cnt + "," + type + "," + x + "," + y + "\n";
                printWriter.print(line);

            }
            printWriter.close();
        } // end try block
        catch (Exception e) {
            System.out.println(e.getClass());
        }



    }



    double[] divCoordScratch=new double[2];


    public Dish(int sideLen,int startingPop,int startingStroma,double startingRadius, String path_to_file){
        super(sideLen,sideLen,Cell.class);
        double[] startCoords=new double[2];
        for (int i = 0; i < CELL_RAD.length ; i++) {
            MAX_RAD[i]=Math.sqrt(2)*CELL_RAD[i];
            DIV_RADIUS[i]=CELL_RAD[i]*(2.0/3.0);
        }

        BloodVesselsCoord = this.addBloodVessels(this.vessels_grid_x, this.vessels_grid_y);

        double[] startCoordsStroma={10,10};

//        startingStroma=(int)Math.round((xDim*yDim)/10);
//        int inc=10;
//        int xStr=0;
//        while(xStr < xDim) {
//            xStr=xStr+inc;
//            int yStr=0;
//            while(yStr<yDim){
//                yStr=yStr+inc;
//                Cell c=NewAgentPT(xStr,yStr);
//                c.Init(2);
//                stromaCells.add(c);}
//        }

//        for (int i = 0; i < startingStroma; i++) {
////            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
//            Cell c=NewAgentPT(Math.round(Math.random()*xDim),Math.round(Math.random()*yDim));
//            c.Init(2);
//            stromaCells.add(c);
//        }

        if (path_to_file == null) {
            for (int i = 0; i < startingStroma; i++) {
//            Utils.RandomPointInCircle(startingRadius, startCoords, rn);
                Cell c = NewAgentPT(Math.random() * xDim, Math.random() * yDim);
                c.Init(2);
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
            List<String> cell_list = readCellCoords(path_to_file);
            // create seeds
            for (int i = 2; i < cell_list.size(); i++) {
            //for (int i = 950; i < 960; i++) {
                String line = cell_list.get(i);
                String[] line_array = line.split(",");
                System.out.println(line);

                double x = Double.parseDouble(line_array[2]);
                double y = Double.parseDouble(line_array[3]);
                int tmp_type = Integer.parseInt(line_array[1]);

                // RECODE CELL IDS FROM NICOLAS' SCHEME TO INTERNAL
                int type = 10;
                switch (tmp_type) {
                    case 1:  type = 1;      // Immune (1) -> (1)
                        break;
                    case 2:  type = 0;      // Melanocyte (2) ->(0)
                        break;
                    case 3:  type = 2;      // STROMA (3) -> (2)
                        break;
                    case 0:  type = 10;     // type > 9 -> ignore
                        break;
                }


                x = x / 500 * xDim;
                y = y / 500 * yDim;
                if (x < xDim && y < yDim && type < 10){
                    System.out.println("----- seeding");
                    System.out.println(x);
                    System.out.println(y);
                    System.out.println(type);

                    Cell c=NewAgentPT(x,y);
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
        if ((tick % 200)==1){
            for (int i = 0; i <BloodVesselsCoord.size() ; i++) {
            Cell c=NewAgentPT(BloodVesselsCoord.get(i)[0],BloodVesselsCoord.get(i)[1]);
            c.Init(1); //immune
            immuneCells.add(c);
            }
        }
    }

    void Production() {

        for (Cell ty : cancerCells) {
            diffusible.Set(ty.Xsq(), ty.Ysq(), diffusible.Get(ty.Xsq(), ty.Ysq()) + productionRate);
        }
    }
    void ChemDecay() {
        for (int x = 0; x < diffusible.xDim; x++) {
            for (int y = 0; y < diffusible.yDim; y++) {
                diffusible.Set(x, y, Math.exp(-decayRate)*diffusible.Get(x,y));
            }
        }
    }

    void Diffusion(){
        //diff.Advection2ndLW(xVels, yVels);
        for (int i = 0; i < 1; i++) {
            diffusible.DiffusionADI(1,0);
            //diffusible.Advection2ndPredCorr(0.1, 0.1);
        }
    }


    void Step(){

        //CHEMICALS
        Diffusion();
        Production();
        ChemDecay();

        //CELLS
        //division
        //death
        //move




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
    double oldProb;
    static double[] motility={0.01,0.01,0.1,0.1};
    double xVelStart;
    double yVelStart;
    double deviation;
    double maxVelAbs;
    Cell cc;
    int immunePotential;
    boolean checkTouch;




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
        if (type==1){ //immunecells initialize to zero the immune potential
            immunePotential=0;
        }
    }

    void SetCellColor(){

        switch (type) {
            case 0:  color = G().RED;      // MELANOMA
                break;
            case 1:  color = G().AZUL;     // IMMUNE
                break;
            case 2:  color = G().YELLOW;   // STROMA    
                break;
//            case 3:  color = G().YELLOW;   // STROMA
//                break;
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
        double x=xVel+Xpt();
        double y=yVel+Ypt();
        if(!G().In(x,y)){
            Dispose();
            return;
        }
        //ForceMove();
        MoveSafePT(x,y);
        ApplyFriction(G().FRICTION);
    }
    void Step(){

        switch (type) {
            case 0:
                Cell killer=checkTouchingImmuneCells();// returns true if in the circle around a cancer cell founds at least one immune cell
                if (checkTouch==true){
                    G().deathProb[type]=0.5;//G().deathProb[type]*10; // if immune cell is present, deathProb is temporarily increased
                    if(G().rn.nextDouble()<G().deathProb[type]){  /// calculate chance of death
                        Dispose();
                        immuneExhaustion(killer);
                        return;
                    }
                    G().deathProb[0]=G().baseCaDeathProb; // return to baseline death prob
                }
                break;
            default:
                 if(G().rn.nextDouble()<G().deathProb[type]){  /// calculate chance of death
                    Dispose();
                    return;
                 }
                 break;
        }

        removeImmuneExhausted(this);


        if(G().rn.nextDouble()<G().divProb[type]){
            Cell child=Divide(G().DIV_RADIUS[type],G().divCoordScratch,G().rn);
            child.Init(this.type);
            Init(this.type);
        }
        moveCell();
//        checkOutOfBorder();
    }
    Cell checkTouchingImmuneCells() {
        checkTouch = false;
        int[] hood = CircleHood(false, 1);  // this circle is centered on the origin, I then need to translate it to the center of my cancer cell (this)
        for (int j = 0; j < (hood.length / 2); j++) { // replaces the hood arount the cancer cell pos
            hood[2 * j] = (int) Math.round(hood[2 * j] + this.Xpt());
            hood[2 * j + 1] = (int) Math.round(hood[2 * j + 1] + this.Ypt());
        }
        int n = 0;//moves on immune cells
        while ((n < G().immuneCells.size()) && (checkTouch == false)) { // considers one at time all cc immune cells
            cc = G().immuneCells.get(n);
            int ccXpt = (int) Math.round(cc.Xpt());
            int ccYpt = (int) Math.round(cc.Ypt());
            if ((ccXpt == 52) && (ccYpt == 52)) {
                int ciao = 1;
            }
            int j = 0; //counts on hood
            while ((checkTouch == false) && (j < (hood.length / 2))) {
                int hoodXpt = hood[2 * j];
                int hoodYpt = hood[2 * j + 1];
                if ((hoodXpt == ccXpt) && (hoodYpt == ccYpt)) {
                    checkTouch = true;
                }
                j = j + 1;
            }
            n = n + 1;
        }
        return cc;
    }

//        for (Cell cc:G().immuneCells)
//            while((toReturn==false)&&(i <(hood.length/2))) {
//                if ((hood[2*i]+this.Xpt()==cc.Xpt())&&(hood[2*i+1]+this.Ypt()==cc.Ypt())){
//                    toReturn=true;
//                }
//                i=i+1;
//            }

    void immuneExhaustion(Cell tkiller){
        if (tkiller.type==1){ // considering immune cell
            tkiller.immunePotential=tkiller.immunePotential+1;
        }
    }

    void removeImmuneExhausted(Cell aImmuneCell){
        if ((aImmuneCell.type==1)&&(aImmuneCell.immunePotential>=5)) {
            aImmuneCell.Dispose();
        }
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
//    void checkOutOfBorder(){
//        if ((this.Xpt()==G().xDim-1)||(this.Xpt()==1)||(this.Ypt()==G().yDim-1)||(this.Ypt()==1)){
//            this.Dispose();
//        }
//    }

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
    static int STARTING_POP=400;
    static int STARTING_STROMA=170;
    static double STARTING_RADIUS=20;
    static int TIMESTEPS=1;


    static float[] circleCoords=Utils.GenCirclePoints(1,10);
    public static void main(String[] args) {
         double[] motility={1,1,1,1};

        //TickTimer trt=new TickRateTimer();
        Vis2DOpenGL vis=new Vis2DOpenGL("Cell Fusion Visualization", 1000,1000,SIDE_LEN,SIDE_LEN);
        
        String path_to_input_file = "Models/MelanomaWorkshop/Model2D/spatial_distribution/stroma_clusters.txt";
        String path_to_output_file = "Models/MelanomaWorkshop/Model2D/spatial_distribution/simulation_output.txt";
        //List<String> list = d.initImageFile(path_to_input_file);

        Dish d=new Dish(SIDE_LEN,STARTING_POP,STARTING_STROMA, STARTING_RADIUS, null);
        GridVisWindow win = new GridVisWindow("diffusion", d.xDim, d.yDim, 5);
        //d.SetCellsColor("red");


        for (int i = 0; i < TIMESTEPS; i++) {
            vis.TickPause(0);
            win.TickPause(0);
            d.Step();
            DrawCells(vis,d);
            DrawDiffusible(win,d);
            if (i == TIMESTEPS-1){
                d.writeCellCoords( path_to_output_file );
            }
        }
    }

    static void DrawCells(Vis2DOpenGL vis,Dish d){

        vis.Clear(Dish.BLACK);

        System.out.println(d.BloodVesselsCoord.size());
        for (int i = 0; i < d.BloodVesselsCoord.size(); i++) {
            vis.Circle(d.BloodVesselsCoord.get(i)[0], d.BloodVesselsCoord.get(i)[1], 2, d.BLUE);
        }

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


    static void DrawDiffusible(GridVisWindow win,Dish d){
        for (int x = 0; x < win.xDim; x++) {
            for (int y = 0; y < win.yDim; y++) {
                win.SetPix(x, y, HeatMapBRG(d.diffusible.Get(x , y )));
            }
        }
    }
}
