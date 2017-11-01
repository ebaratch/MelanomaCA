package Models.JeffreyModels.DeterministicGame;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Gui.GridVisWindow;
import Framework.Tools.Binomial;
import Framework.Tools.FileIO;
import Framework.Gui.GifMaker;


import java.util.Random;

import static Framework.Utils.*;

class Tissue extends AgentGrid2D<Cell> {

    public static int IMMUNOSUPPRESSIVE = 0;
    public static int RESISTANT = 1;
    public static int NORMAL = 2;

    // cell params
    double DEATH_RATE; double GEN_MUT_RATE; int TOTAL_CELLS;
    double A; double B; double C;
    double D; double E; double F;
    double G; double H; double I;
    double AA; double BB; double CC;

    // fitness hoods
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];

    /////////////////////////////////////////////
    // SPATIAL CIRCLES
    /////////////////////////////////////////////

    private int[]circleHood=CircleHood(true, 6);
    private int[]circleArrayIs=new int[circleHood.length/2];


    // circle hoods
//    private int[]largeCircleHood=CircleHood(true, 7);
//    private int[]largeCircleArrayIs=new int[largeCircleHood.length/2];
//
//    // smaller circle
//    private int[]smallCircleHood=CircleHood(true, 4);
//    private int[]smallCircleArrayIs=new int[smallCircleHood.length/2];


    /////////////////////////////////////////////
    // SPATIAL SQUARES
    /////////////////////////////////////////////
//    private int[]largeRectHood=RectangleHood(true, 5, 5);
//    private int[]largeRectArrayIs=new int[largeRectHood.length/2];
//
//    // smaller circle
//    private int[]smallRectHood=RectangleHood(true, 3, 3);
//    private int[]smallRectArrayIs=new int[smallRectHood.length/2];

    Random rn=new Random();

    Tissue(int sideLen, double death_rate, double gen_mut_rate, double a, double b, double c, double d, double e, double f, double g, double h, double i, double aa, double bb, double cc, boolean circles){
        super(sideLen,sideLen, Models.JeffreyModels.DeterministicGame.Cell.class,true,true);

        DEATH_RATE = death_rate;
        GEN_MUT_RATE = gen_mut_rate;
        TOTAL_CELLS = sideLen * sideLen;
        A = a;  B = b; C = c; D = d; E = e; F = f; G = g; H = h; I = i; AA = aa; BB = bb; CC = cc;

//        // FILL EVERYTING RANDOM TYPE LOLZ
//        for (int ii=0;ii<TOTAL_CELLS;ii++) {
//            Cell cell=NewAgentSQ(ii);
//            int nextType = rn.nextInt(3);
//            cell.Init(nextType);
//        }


        // fill everything with normal (2)
        for (int ii=0;ii<TOTAL_CELLS;ii++) {
            Cell cell=NewAgentSQ(ii);
            cell.Init(NORMAL);
        }

        int smallerPop = HoodToOccupiedIs(circleHood,circleArrayIs,sideLen/2,sideLen/2);
        for (int j = 0; j < smallerPop; j++) {
            Cell circleMember = GetAgent(circleArrayIs[j]);
            circleMember.Init(IMMUNOSUPPRESSIVE);
        }

        System.out.println(smallerPop);

//        for (int xx = -5; xx < 5; xx++) {
//            for (int yy = -5; yy < 5; yy++) {
//                Cell cell = GetAgent(xx + sideLen/2,yy + sideLen/2);
//                cell.Init(IMMUNOSUPPRESSIVE);
//            }
//        }

        Cell cell = GetAgent(-6  + sideLen/2,sideLen/2);
        cell.Init(RESISTANT);



//        if (circles) {
//            // CIRCLES
//
//            // add in 0th type (LARGE)
//            int largerPop = HoodToOccupiedIs(largeCircleHood,largeCircleArrayIs,sideLen/2,sideLen/2);
//            for (int j = 0; j < largerPop; j++) {
//                Cell circleMember = GetAgent(largeCircleArrayIs[j]);
//                circleMember.myType = 0;
//                circleMember.nextState = 0;
//            }
//
//            // add in 0th type (SMALL)
//            int smallerPop = HoodToOccupiedIs(smallCircleHood,smallCircleArrayIs,sideLen/2,sideLen/2);
//            for (int j = 0; j < smallerPop; j++) {
//                Cell circleMember = GetAgent(smallCircleArrayIs[j]);
//                circleMember.myType = 1;
//                circleMember.nextState = 1;
//            }
//            System.out.println("larger : " + (largerPop - smallerPop) + " smaller : " + smallerPop );
//        } else {
//            // RECTANGLES
//
//            // add in 0th type (LARGE)
//            int largerPop = 0;
//            for (int x = 50; x < 60; x++) {
//                for (int y = 45; y < 55; y++) {
//                    Cell circleMember = GetAgent(x,y);
//                    circleMember.myType = 0;
//                    circleMember.nextState = 0;
//                    largerPop++;
//                }
//            }
//
//            // add in 0th type (SMALL)
//            int smallerPop = HoodToOccupiedIs(smallRectHood,smallRectArrayIs,sideLen/2 - 4,sideLen/2);
//            for (int j = 0; j < smallerPop; j++) {
//                Cell circleMember = GetAgent(smallRectArrayIs[j]);
//                circleMember.myType = 1;
//                circleMember.nextState = 1;
//            }
//            System.out.println("larger : " + (largerPop) + " smaller : " + smallerPop );
//        }
    }

    void Step(){

        // update all fitnesses!
        for (Cell cell : this) {
            cell.CalcFitness();
        }


        // pick binomial ( N cells, death_rate) for death, kill least fit neighbor
        Binomial bn=new Binomial();
        int res=bn.SampleInt(TOTAL_CELLS,DEATH_RATE,rn);
        int i = 0;

        for (Cell c:this) {
            if (i >= res) { break; }
            c.killLeastFitNeighborAndUpdateThatNewCell();
            i++;
        }

        // update next state to cur state
        for (Cell cell : this ) {
            cell.myType = cell.nextState;
        }

        ShuffleAgents(rn);
        IncTick();

    }
}

class Cell extends AgentSQ2Dunstackable<Models.JeffreyModels.DeterministicGame.Tissue> {
    int myType; // 0 1 2
    double myFitness;
    int nextState;

    void Init(int initialType){
        myType = initialType;
        nextState = initialType;
    }

    void killLeastFitNeighborAndUpdateThatNewCell() {
        int neighbors=G().HoodToOccupiedIs(G().neighborhood,G().divIs,Xsq(),Ysq());

        double maxOrMinFitness = 100000.0;
        int uniqueness = 0;
        int neighborIterator = 0;
        double neighborFitnessArray[] = new double[neighbors];

        for (int i = 0; i < neighbors; i++) {
            Cell neighbor = G().GetAgent(G().divIs[i]);
            neighborFitnessArray[i] = neighbor.myFitness;

            if ( neighbor.myFitness < maxOrMinFitness ) {
                neighborIterator = i;
                uniqueness = 1;
                maxOrMinFitness = neighbor.myFitness;
            }
            if (neighbor.myFitness == maxOrMinFitness ) {
                uniqueness += 1;
            }
        }

        // now there are uniqueness # of options to kill.
        int killThis = G().rn.nextInt(uniqueness);

        // kill dead guy
        Cell deadGuy = G().GetAgent(G().divIs[neighborIterator]);

        int j = 0;
        for (int i = 0; i < neighbors; i++) {
            if ( neighborFitnessArray[i] == maxOrMinFitness )  {
                if ( j == killThis) {
                    deadGuy = G().GetAgent(G().divIs[i]);
                }
                j++;
            }
        }



        maxOrMinFitness = -100000.0;
        uniqueness = 0;
        neighbors = G().HoodToOccupiedIs(G().neighborhood,G().divIs,deadGuy.Xsq(),deadGuy.Ysq());

        for (int i = 0; i < neighbors; i++) {
            Cell neighbor = G().GetAgent(G().divIs[i]);
            neighborFitnessArray[i] = neighbor.myFitness;

            if ( neighbor.myFitness > maxOrMinFitness ) {
                neighborIterator = i;
                uniqueness = 1;
                maxOrMinFitness = neighbor.myFitness;
            }

            if (neighbor.myFitness == maxOrMinFitness ) {
                uniqueness += 1;
            }
        }

        Cell fitGuy = G().GetAgent(G().divIs[neighborIterator]);
        killThis = G().rn.nextInt(uniqueness);

        j = 0;
        for (int i = 0; i < neighbors; i++) {
            if ( neighborFitnessArray[i] == maxOrMinFitness )  {
                if ( j == killThis) {
                    fitGuy = G().GetAgent(G().divIs[i]);
                }
                j++;
            }
        }


        deadGuy.nextState = fitGuy.myType;

    }


    void CalcFitness() {
        myFitness = 0.0;
        int totalNeighbors=G().HoodToOccupiedIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        double mult1 = G().A;
        double mult2 = G().B;
        double mult3 = G().C;

        if (myType == 0) {
            myFitness += G().AA;
        }

        if (myType == 1) {
            mult1 = G().D;
            mult2 = G().E;
            mult3 = G().F;
            myFitness += G().BB;
        }

        if (myType == 2) {
            mult1 = G().G;
            mult2 = G().H;
            mult3 = G().I;
            myFitness += G().CC;
        }

        for (int i = 0; i < totalNeighbors; i++) {
            Cell neighbor = G().GetAgent(G().divIs[i]);

            if (neighbor.myType == 0) { myFitness+=mult1; }
            if (neighbor.myType == 1) { myFitness+=mult2; }
            if (neighbor.myType == 2) { myFitness+=mult3; }
        }
    }

}


public class DeterministicGame {


    ///////////////////////////////////
    // changeable parameters
    ///////////////////////////////////

    static int sim = 1;
    static int scenario = 2; // EIR = 1 or IRN = 2
    static boolean circles = true;
    static int visualSpeed = 10;

    // static parameters
    static int sideLen =100;


    static double BENEFIT_G = 0.5;
    static double BENEFIT_N = 0.5;
    static double COST_ROS = -0.5;
    static double COST_STRAT = -0.2;

    static double death_rate = 0.1;
    static double gen_mut_rate = 0.01;

    public static void main(String[] args) {
        if (sim == 0) {
            BENEFIT_G = 0.5;
            BENEFIT_N = 0.5;
            COST_ROS = -0.5;
            COST_STRAT = -0.2; // C_I
        }

        if (sim == 1) {
            BENEFIT_G = 0.5;
            BENEFIT_N = 0.01;
            COST_ROS = -0.5;
            COST_STRAT = -0.2;
        }

        String baseFilename;

        if (circles) {
            if (scenario == 1) {
                baseFilename = "circles_EIR_sim" + Integer.toString(sim) + "_";
            } else {
                baseFilename = "circles_IRN_sim" + Integer.toString(sim) + "_";
            }
        } else {
            if (scenario == 1) {
                baseFilename = "squares_EIR_sim" + Integer.toString(sim) + "_";
            } else {
                baseFilename = "squares_IRN_sim" + Integer.toString(sim) + "_";
            }
        }

        String tumorFilename = "DATA_" + baseFilename + ".csv";
        FileIO tumorOutputFile = new FileIO(tumorFilename, "w");
        String gifName = "GIF" + baseFilename + ".gif";
        GifMaker myGif = new GifMaker(gifName, 100,true);


        int printArray[] = new int[]{0, 20, 50, 100, 200, 300, 400, 500, 1000};
        int printI = 0;
        int nTicks=printArray[printArray.length-1];

        GridVisWindow win = new GridVisWindow("Deterministic Game", sideLen, sideLen, 6, true);
        Tissue t;

        if (scenario == 1) {
            // E I R
            t =  new Tissue(sideLen, death_rate, gen_mut_rate, 0.0, BENEFIT_G, 0.0, 0.0, BENEFIT_G, 0.0, 0.0, BENEFIT_G, 0.0, COST_STRAT, (BENEFIT_G + COST_STRAT), COST_STRAT, circles);
        } else {
            // I R N
            t =  new Tissue(sideLen, death_rate, gen_mut_rate, BENEFIT_G, 0.0, BENEFIT_N, BENEFIT_G, 0.0, BENEFIT_N, BENEFIT_G, COST_ROS, 0.0, (BENEFIT_G + COST_STRAT), COST_STRAT, 0, circles);
        }





        for (int i = 0; i <= nTicks; i++) {
            // draw
            win.TickPause(visualSpeed);
            t.Step();
            Draw(t,win, scenario);
            //System.out.println(i);

            if (i % 10 == 0) {
                myGif.AddFrame(win);
            }

            if (i == printArray[printI]) {
                // print
                String imageFilename = "IMAGE_" + baseFilename + Integer.toString(printArray[printI]) + ".png";
                win.ToPNG(imageFilename);
                printI++;
            }
            OutputPopSizes(t, tumorOutputFile, i);
        }

        tumorOutputFile.Close();
        myGif.Close();
        win.Dispose();
    }
    static void Draw(Tissue t, GridVisWindow win, int scenario) {

        if (scenario == 1) {
            for (int i = 0; i < win.length; i++) {
                Cell c = t.GetAgent(i);
                if(c==null){
                    win.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
                }
                else if (c.myType == 0){
                    win.SetPix(i, RGB(78.0/255, 173.0/255, 234.0/255));
                } else if (c.myType == 1){
                    win.SetPix(i, RGB(244.0/255, 193.0/255, 45.0/255));
                } else if (c.myType == 2){
                    win.SetPix(i, RGB(176.0/255, 36.0/255, 24.0/255));
                }
            }
        } else {
            for (int i = 0; i < win.length; i++) {
                Cell c = t.GetAgent(i);
                if(c==null){
                    win.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
                }
                else if (c.myType == 0){
                    win.SetPix(i, RGB(244.0/255, 193.0/255, 45.0/255));
                } else if (c.myType == 1){
                    win.SetPix(i, RGB(176.0/255, 36.0/255, 24.0/255));
                } else if (c.myType == 2){
                    win.SetPix(i, RGB(164.0/255, 73.0/255, 235.0/255));
                }
            }
        }
    }

    static void OutputPopSizes(Tissue t, FileIO out, int time){

        int typesArray[] = new int[]{time,0,0,0};

        for (Cell cell : t) {
            typesArray[cell.myType+1]++;
        }

        out.Write(ArrToString(typesArray,",")+"\n");
    }
}

