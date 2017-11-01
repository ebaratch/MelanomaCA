package Models.JeffreyModels.GetJeffrey;

import Framework.Gui.GifMaker;
import Framework.Gui.GuiGridVis;
import Framework.Gui.TickTimer;

import java.util.Random;
import static Framework.Utils.ArrToString;
import static Framework.Utils.CircleHood;
import static Framework.Utils.MooreHood;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;

import Framework.Gui.GuiWindow;
import Framework.Utils;

class Cell extends AgentSQ2Dunstackable<Jeffrey> {
    int k;
    boolean outside;

    void Init(int k0, boolean outside){
        this.k = k0;
        this.outside = outside;
    }
    void Mutate(){
        if((G().rn.nextDouble()<G().GENE_MUT_RATE) && (k < G().KMAX)){
            k++;
        }
    }

    double GetBirthProbability(){
        return G().BIRTH_RATE*Math.pow((1+G().S),(double)k);
    }

    Cell Divide(){
        int nDivOptions=G().HoodToEmptyIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        if(nDivOptions==0){ return null; }
        Cell daughter=G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivOptions)]);

        if (isNotInJ(daughter)) {
            daughter.outside = true;
            return daughter;
            //daughter.Dispose();
            // return null;
        } else {
            daughter.Init(k, false);
            daughter.Mutate();
            Mutate();
            return daughter;
        }


    }

    boolean isNotInJ(Cell c) {
        boolean XNotInJ = true;
        boolean YNotInJ = true;

        for (int i = 0; i < G().jNeighborhoodX.length; i++) {
            if (c.Xsq() == G().jNeighborhoodX[i]) {
                // check *this* y position
                if (c.Ysq() == G().jNeighborhoodY[i] ) {
                    return false;
                }
            }
        }
        return true;
    }

    void Step(){

        // check if birth event
        if(G().rn.nextDouble()<GetBirthProbability()){
            Divide();
        }
    }
}

public class Jeffrey extends AgentGrid2D<Cell> {

    // cell params
    double S;
    double BIRTH_RATE;
    double GENE_MUT_RATE;
    int K0 = 1;
    int KMAX;
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];
    Random rn=new Random();


    // set up J
    int n1 = 40, m1 = 6, delX1 = 0, delY1 = 0;
    int n2 = 6, m2 = 40, delX2 = 0, delY2 = 20;
    int n3 = 6, m3 = 24, delX3 = -9, delY3 = -20;
    int n4 = 2, m4 = 4, delX4 = -19, delY4 = -16;

    int arrayLength = n1*m1 + n2*m2 + n3*m3 + n4*m4;

    int[] jNeighborhoodX = new int[arrayLength];
    int[] jNeighborhoodY = new int[arrayLength];

    // cell parameters
    static int sideLenX = 100;
    static int sideLenY = 100;

    Jeffrey(int sideLenX, int sideLenY, double thisS, double thisBirthRate, double thisGeneMutRate, int thisKMAX){
        super(sideLenX,sideLenY, Cell.class,true,true);

        S = thisS;
        BIRTH_RATE = thisBirthRate;
        GENE_MUT_RATE = thisGeneMutRate;
        KMAX = thisKMAX;

        Cell c = NewAgentPT(sideLenX / 2, sideLenY / 2);
        c.k = K0;


        int curInd = AddRectToJ(n1,m1,delX1,delY1,0);
        curInd = AddRectToJ(n2,m2,delX2,delY2, curInd);
        curInd = AddRectToJ(n3,m3,delX3,delY3, curInd);
        curInd = AddRectToJ(n4,m4,delX4,delY4, curInd);

    }

    int AddRectToJ(int n, int m, int delX, int delY, int startingInd) {
        int ind = startingInd;
        // build J neighborhood
        for (int i = -(m / 2); i < (m / 2); i++) {
            for (int j = -(n / 2); j < (n / 2); j++) {
                jNeighborhoodX[ind] = sideLenX/2 + i + delX;
                jNeighborhoodY[ind] = sideLenY/2 + j + delY;
                ind++;
            }
        }
        return ind;
    }

    void OriginalStep(){
        for (Cell c:this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }

    public static void RunAndVisualize(double S, double birthRate, double geneMut, int kMax, int totalTime) {
        GuiWindow win = new GuiWindow("Metastatic", true);
        GuiGridVis Vis = new GuiGridVis(sideLenX, sideLenY, 5);
        win.AddCol(0, Vis);
        win.RunGui();
        TickTimer tt = new TickTimer();

        //set up primary
        Jeffrey model = new Jeffrey(sideLenX, sideLenY, S, birthRate, geneMut, kMax);

        // make gif
        String baseFilename = "awesomeGif.gif";
        GifMaker myGif = new GifMaker(baseFilename, 100,true);

        for (int time = 0; time < totalTime; time++) {
            DrawCells(model, Vis);
            tt.TickPause(100);
            model.OriginalStep();

            myGif.AddFrame(Vis);
        }
        myGif.Close();
    }

    public static void main(String[] args) {
        double S = 0.1;
        double birthRate = 0.5;
        double geneMut = 0.05;
        int kMax = 10;
        int r0 = 5;

        RunAndVisualize(S,birthRate,geneMut,kMax,80);

    }

    public static void DrawCells(Jeffrey model, GuiGridVis visCells) {
        for (int i = 0; i < visCells.length; i++) {
            Cell c=model.GetAgent(i);
            if(c==null){
                //background color (black)
                visCells.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 0));
            } else{
                if (c.outside) {
                    // 66, 134, 244
                    Random rando=new Random();
                    visCells.SetPix(i,Utils.RGB(66.0/255.0, 134.0/255.0, (150.0 + (rando.nextDouble()*100.0) )/255.0));
                } else {
                    if (c.k == 1) {
                        visCells.SetPix(i,Utils.RGB(218.0/255.0, 247.0/255.0, 166.0/255.0));
                    } else if (c.k == 2) {
                        visCells.SetPix(i,Utils.RGB((double) 1, 195.0/255.0, (double) 0));
                    } else if (c.k == 3) {
                        visCells.SetPix(i,Utils.RGB((double) 1, 87.0/255.0, 51.0/255.0));
                    } else if (c.k == 4) {
                        visCells.SetPix(i,Utils.RGB((double) 1, 51.0/255.0, 153.0/255.0));
                    } else {
                        visCells.SetPix(i, Utils.RGB(88.0/255.0, 24.0/255.0, 69.0/255.0));
                    }
                }


            }

        }
    }
}
