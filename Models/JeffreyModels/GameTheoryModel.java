package Models.JeffreyModels;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

import java.util.Random;

import static Framework.Utils.*;

class TissueGrid extends AgentGrid2D<Player> {

    int m;
    double b;

    //System.out.println("\n");

    // payoff entries
    double AA = 1.0;
    double BB = 0.0;
    double DD = 0;
    double GENE_MUT_RATE=0.0001;

    int cellCount = 0;

    int[]neighborhoodWithOrigin;
    int[]neighborhoodWithoutOrigin;
    int[]divIsWithOrigin;
    int[]divIsWithoutOrigin;

//    int neighborhoodDimension = 1;
//    int[]neighborhoodWithOrigin=RectangleHood(true,neighborhoodDimension,neighborhoodDimension);
//    int[]neighborhoodWithoutOrigin=RectangleHood(false,neighborhoodDimension,neighborhoodDimension);
//
//    int[]divIsWithOrigin=new int[neighborhoodWithOrigin.length/2];
//    int[]divIsWithoutOrigin=new int[neighborhoodWithoutOrigin.length/2];

    Random rn=new Random();

    TissueGrid(int xLen, int yLen,int startPop, int thism, double thisb, int neighborhoodDimension){
        super(xLen,yLen,Player.class,true,true);
        m = thism;
        b = thisb;
        int[] ret= GenIndicesArray(xDim*yDim);
        Shuffle(ret, xDim*yDim, startPop, rn);
        int[]startIs= ret;

        neighborhoodWithOrigin= RectangleHood(true,neighborhoodDimension,neighborhoodDimension);
        neighborhoodWithoutOrigin= RectangleHood(false,neighborhoodDimension,neighborhoodDimension);

        divIsWithOrigin=new int[neighborhoodWithOrigin.length/2];
        divIsWithoutOrigin=new int[neighborhoodWithoutOrigin.length/2];


        // init startPop of grid with CANCER (1) type

        for (int i=0;i<startPop;i++) {
            Player c=NewAgentSQ(startIs[i]);
            c.Init(1);
        }

        // init rest of typeGrid with NORMAL (0) type
        for (int i = startPop; i < (xDim*yDim); i++) {
            Player c=NewAgentSQ(startIs[i]);
            c.Init(0);
        }
    }

    static int[] DeepMooreHood(boolean includeOrigin,int deepness) {
        if (includeOrigin) {
            int[]myNeighborhood = new int[(2*deepness + 1)*(2*deepness + 1)*2];
            int iter = 0;
            for (int row = -deepness; row <= deepness ; row++) {
                for (int col = -deepness; col <= deepness; col++) {
                    myNeighborhood[iter] = row;
                    myNeighborhood[iter+1] = col;
                    iter+=2;
                }
            }

            return myNeighborhood;
        } else {
            int[]myNeighborhood = new int[(2*deepness + 1)*(2*deepness + 1)*2];
            int iter = 0;
            for (int row = -deepness; row <= deepness ; row++) {
                for (int col = -deepness; col <= deepness; col++) {
                    if ((row + col) != 0) {
                        myNeighborhood[iter] = row;
                        myNeighborhood[iter+1] = col;
                        iter += 2;
                    }
                }
            }

            return myNeighborhood;
        }
    }

    void StepBefore(){
        // update fitness values
        for (Player c:this) {
            c.CalcFitness(this.b);
        }

        // update Next StepModules;
        for (Player c:this) {
            c.UpdateNextState(this.m);
        }

    }
    void StepAfter(){
        // update Current StepModules;
        for (Player c:this) {
            c.UpdateCurrentState();
        }

        IncTick();
    }
}

class Player extends AgentSQ2Dunstackable<TissueGrid> {

    int myType = 0;// type of 0 or 1 (normal or cancer)
    int nextType = 0;
    double myFitness = 0.0;

    void Init(int thisType){
        myType = thisType;
        nextType = thisType;
        myFitness = 0.0;
    }

    void CheckMutatation(){
        if((myType==0)&&G().rn.nextDouble()<G().GENE_MUT_RATE){
            myType = 1;
        }
    }
    void UpdateNextState(int m) {

        // calculate probability of being a cancer cell
        int neighborIndices=G().HoodToOccupiedIs(G().neighborhoodWithOrigin,G().divIsWithOrigin,Xsq(),Ysq());
        double cancerFitness = 0.0;
        double totalFitness = 0.0;

        for (int i = 0; i < neighborIndices; i++) {
            Player neighbor = G().GetAgent(G().divIsWithOrigin[i]);

            totalFitness+=Math.pow(neighbor.myFitness,m);
            cancerFitness+=(neighbor.myType == 1)?Math.pow(neighbor.myFitness,m):0;
        }

        double Pc = cancerFitness/totalFitness;
        nextType = (G().rn.nextDouble() < Pc) ? 1 : 0;
    }
    void UpdateCurrentState() {
        myType = nextType;
        CheckMutatation();
    }
    void CalcFitness(double b) {
        myFitness = 0.0;
        int totalNeighbors=G().HoodToOccupiedIs(G().neighborhoodWithoutOrigin,G().divIsWithoutOrigin,Xsq(),Ysq());
        double mult1 = G().AA;
        double mult2 = G().BB;

        if (myType == 1) {
            mult1 = b;
            mult2 = G().DD;
        }

        for (int i = 0; i < totalNeighbors; i++) {
            Player neighbor = G().GetAgent(G().divIsWithoutOrigin[i]);
            myFitness+=(neighbor.myType == 0)?mult1:mult2;
        }
    }
}

public class GameTheoryModel {
    static int xLen =80;
    static int yLen = 80;
    static int nTicks=100;
    static int mm = 1;
    static double bb = 1.35;

    public static void main(String[] args) {
        // new window and labels at the top
        GuiWindow win=new GuiWindow("Game_Theory",true);

        int[] mvec = new int[]{1};
        //double[] bvec = new double[]{1.05, 1.13,1.16,1.35,1.42,1.55,1.71,1.77,1.9,2.01};

        int[] NDvec = new int[]{1, 2, 3, 4, 5};
        double[] bvec = new double[]{1.35};

        int totalGrids = NDvec.length;
        TissueGrid[] TissueGrids = new TissueGrid[totalGrids];
        GuiGridVis[] Visies = new GuiGridVis[totalGrids];

        // create new GridsAndAgents and Visies
        win.AddCol(0, new GuiLabel("Jeffrey"));//space

        for (int i1 = 0; i1 < NDvec.length; i1++) {
            win.AddCol(0, new GuiLabel("ND = " + mvec[i1]));
            win.AddCol(0, new GuiLabel(" "));
        }



        for (int i1 = 0; i1 < mvec.length; i1++) {
            win.AddCol(0, new GuiLabel("m = " + mvec[i1]));
            win.AddCol(0, new GuiLabel(" "));
        }
        int iter = 0;
        //for (int i1 = 0; i1 < mvec.length; i1++) {
        for (int i1 = 0; i1 < NDvec.length; i1++) {
            for (int i2 = 0; i2 < bvec.length; i2++) {

                if (i1 == 0) {
                    win.AddCol(i2*2+1, new GuiLabel("b = " + bvec[i2]));
                }

                int neighborhoodDimension = 1;

                TissueGrids[iter]=new TissueGrid(xLen, yLen,1,mvec[i1],bvec[i2],neighborhoodDimension);
                Visies[iter] = new GuiGridVis(xLen,yLen,2,1,1,true);

                // add to column
                win.AddCol(i2*2+1, Visies[iter]);
                win.AddCol(i2*2+2, new GuiLabel("    "));
                win.AddCol(i2*2+1, new GuiLabel(" "));

                iter++;
            }
        }


        for (int i1 = 0; i1 < NDvec.length; i1++) {

                if (i1 == 0) {
                    win.AddCol(i1*2+1, new GuiLabel("N = " + NDvec[i1]));
                }

                TissueGrids[i1]=new TissueGrid(xLen, yLen,1,mm,bb,NDvec[i1]);
                Visies[i1] = new GuiGridVis(xLen,yLen,2,1,1,true);

                // add to column
                win.AddCol(i1*2+1, Visies[i1]);
                win.AddCol(i1*2+2, new GuiLabel("    "));
                win.AddCol(i1*2+1, new GuiLabel(" "));

        }

        win.RunGui();
        TickTimer trt=new TickTimer();

        for (int i = 0; i < nTicks; i++) {
            trt.TickPause(0);

            for (int i1 = 0; i1 < NDvec.length; i1++) {
                TissueGrids[i1].StepBefore();
                Visies[i1].DrawAgents(TissueGrids[i1], GameTheoryModel::PlayerToColor, RGB((double) 0, (double) 0, (double) 0));
                TissueGrids[i1].StepAfter();
            }


//            iter = 0;
//            for (int i1 = 0; i1 < mvec.length; i1++) {
//                for (int i2 = 0; i2 < bvec.length; i2++) {
//                    TissueGrids[iter].StepBefore();
//                    Visies[iter].DrawAgents(TissueGrids[iter],GameTheoryModel::PlayerToColor,0,0,0);
//                    TissueGrids[iter].StepAfter();
//                    iter++;
//                }
//            }
            System.out.println(i);
        }
        //win.Dispose();

    }
    static int PlayerToColor(Player c){

        // draw according to next step*
        if ((c.myType == 0)&&(c.nextType == 0)) {
            // color red
            return RGB(0,0,1);
        } else if ((c.myType == 0)&&(c.nextType == 1)) {
            // green
            return RGB(0,1,0);
        } else if ((c.myType == 1)&&(c.nextType == 0)) {
            // yellow
            return RGB(1,1,0);
        } else {
            // color blue
            return RGB(1,0,0);
        }

//        return RGB(1,(1.0-c.myType),0);
    }


//    static void OutputPopSizes(Tissue t, FileIO out){
//        Arrays.fill(t.MutantCts,0);
//        for (Cell c : t) {
//            int nMuts=0;
//            for (boolean gene : c.genome) {
//                if(gene){nMuts++;}
//            }
//            t.MutantCts[nMuts]++;
//        }
//        out.Write(ArrToString(t.MutantCts,",")+"\n");
//    }


}
