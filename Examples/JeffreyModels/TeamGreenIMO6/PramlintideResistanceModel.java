package Examples.JeffreyModels.TeamGreenIMO6;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.Grid2D;
import Framework.Gui.GifMaker;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Interfaces.TreatableTumor;
import Framework.Tools.FileIO;
import static Framework.Utils.*;
//import static Framework.Utils.ArrToString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

// read in CSV
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;



class Cell extends AgentSQ2Dunstackable<PramlintideResistanceModel> {
    int clonalID;
    int ticks_until_division;
    Grid2D V;
    Grid2D W;
    Grid2D drug_column;
    Grid2D mrna;
    double phenotype;
    boolean res;

    // straightforward constructor
    void Init(int clonalID, boolean resistant){
        this.clonalID = clonalID;
        this.res = resistant;

        this.V = new Grid2D(9,13);
        String filename = G().baseFilepath + "V" + Integer.toString(clonalID) + ".csv";
        this.V = G().readCSV(filename,V);

        this.W = new Grid2D(10,10);
        filename = G().baseFilepath + "W" + Integer.toString(clonalID) + ".csv";
        this.W = G().readCSV(filename,W);

        this.drug_column = new Grid2D(10,1);
        for (int i = 0; i < 10; i++) {
            this.drug_column.Set(i,0,this.W.Get(i,7));
            this.W.Set(i,7,0.0);
        }

        this.mrna = new Grid2D(13,1);
        if (resistant) {
            SetResistant();

        } else {
            SetSensitive();
        };

        drug_column = new Grid2D(10,1);;

    }

    // constructor for daughter copying
    void Init(int clonalID, boolean resistant, Grid2D V, Grid2D W, Grid2D mrna){
        this.clonalID = clonalID;
        this.mrna = mrna;
        this.res = resistant;
        this.V = V;
        this.W = W;


        drug_column = new Grid2D(10,1);
        for (int i = 0; i < 10; i++) {
            this.drug_column.Set(i,0,this.W.Get(i,7));
            this.W.Set(i,7,0.0);
        }

    }

    void SetResistant() {
        String filename =  G().baseFilepath + "MRNAR.csv";
        this.mrna = G().readCSV(filename,this.mrna);
    }

    void SetSensitive() {
        String filename =  G().baseFilepath + "MRNAS.csv";
        this.mrna = G().readCSV(filename,this.mrna);
    }

    void getPhenotype() {
        Grid2D driftedV = this.V;
        Grid2D driftedW = this.W;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 13; j++) {
                driftedV.Set(i,j,driftedV.Get(i,j) + G().rn.nextGaussian()*G().DRIFT);
                if (i < this.W.xDim && j < this.W.yDim) {
                    driftedW.Set(i,j,driftedW.Get(i,j) + G().rn.nextGaussian()*G().DRIFT);
                }
            }
        }


        Grid2D newV = G().A.DotProduct(driftedV);
        Grid2D SS = new Grid2D(10, 1);

        for (int x = 0; x < 10; x++) {
            //fill in values by dot product
            for (int i = 0; i < 13; i++) {
                SS.Add(x,0,newV.Get(x,i)*this.mrna.Get(i,0));
            }
        }

        for (int j = 0; j < 10; j++) {
            SS.Set(j,0,Math.tanh(SS.Get(j,0)));
        }


        for (int i = 0; i < 100; i++) {
            SS = this.W.DotProduct(SS);
            for (int j = 0; j < 10; j++) {
                SS.Set(j,0,Math.tanh(SS.Get(j,0)));
            }
        }

        //G().printGrid2D(SS);
        phenotype = (SS.Get(9,0) + 1.0) / 2.0;

    }

    void applyDrug() {
        for (int i = 0; i < 10; i++) {
            this.drug_column.Set(i,0,this.W.Get(i,7));
            this.W.Set(i,7,0.0);
        }
    }

    void removeDrug() {
        for (int i = 0; i < 10; i++) {
            this.W.Set(i,7,this.drug_column.Get(i,0));
        }
    }

    Cell Divide(){
        int nDivOptions=G().HoodToEmptyIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        if(nDivOptions==0){ return null; }
        Cell daughter=G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivOptions)]);
        daughter.Init(this.clonalID, this.res, this.V, this.W, this.mrna);
        daughter.Mutate();
        return daughter;
    }

    void Mutate() {
        if (G().rn.nextDouble() < G().MUTATION_RATE) {
            int gene = G().rn.nextInt(8);

            for (int i = 0; i < 10; i++) {
                this.W.Set(i,gene, G().rn.nextGaussian());
            }

            clonalID++;
        }
    }


    void Step(boolean drug){

        // update internal phenotype, shuffle
        if (drug) {
            applyDrug();
        }

        getPhenotype();

        if (G().rn.nextDouble() < G().RANDOM_DEATH_RATE) {
            Dispose();
            return;
        }

        if(G().rn.nextDouble()<(1.0 - phenotype)){
            Dispose();
            return;
        }

        // check if birth event
        if(G().rn.nextDouble()<(phenotype)*G().timeFactor){
            Divide();
        }


    }
}

public class PramlintideResistanceModel extends AgentGrid2D<Cell> {


    // set up constant variables
    public static double RANDOM_DEATH_RATE = 0.005;
    public static double MUTATION_RATE = 0.0000524;
    public static int clonal_id = 0;
    public static double DRIFT = 0.001;
    Random rn=new Random();

    public static double timeFactor = 0.5;

    // some public gridz
    public static Grid2D A = new Grid2D(13,9);
    public static Grid2D MRNAS = new Grid2D(13,1);
    public static Grid2D MRNAR = new Grid2D(13,1);
    public static String baseFilepath = "/Users/westjb/Dropbox/Moffitt Research/Pramlintide Resistance/data/";

    // neighborhoods
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];



    PramlintideResistanceModel(int sideLen) {
        super(sideLen,sideLen, Cell.class,true,true);

        int cellLine = 9;
        //int cellLine = 242;
        Cell cell2 = NewAgentSQ(50 + sideLen/2,50 + sideLen/2);
        cell2.Init(cellLine,true);
        cell2.getPhenotype();
        System.out.println(cell2.phenotype);
        cell2.applyDrug();
        cell2.getPhenotype();
        System.out.println(cell2.phenotype);


        for (int i = -5; i < 5; i++) {
            for (int j = -5; j <5; j++) {
                Cell cell = NewAgentSQ(i + sideLen/2,j + sideLen/2);
                //cell.Init(953,false);

                if (rn.nextDouble() < 0.01) {
                    cell.Init(cellLine,true);
                } else {
                    cell.Init(cellLine,false);
                }
            }
        }


    }

    void Step(boolean drug){
        for (Cell c:this) {
            c.Step(drug);
        }
        CleanShuffInc(rn);
    }

    public static void main(String[] args) {

        int sideLen = 200;
        PramlintideResistanceModel model = new PramlintideResistanceModel(sideLen);


        String csvFilename = baseFilepath + "A.csv";
        A = readCSV(csvFilename,A);

        GuiWindow win = new GuiWindow("Pramlintide", true);
        GuiGridVis Vis = new GuiGridVis(sideLen, sideLen, 3);
        GuiGridVis Vis2 = new GuiGridVis(sideLen, sideLen, 3);
        win.AddCol(0, Vis);
        win.AddCol(1, Vis2);

        win.RunGui();

        TickTimer tt = new TickTimer();

        for (int time = 0; time < 1000; time++) {
            //System.out.println(time);
            tt.TickPause(0);

            if (time > 30) {
                model.Step(true);
                DrawCells(model, Vis,true, true);
                DrawCells(model, Vis2,true, false);
            } else {
                model.Step(false);
                DrawCells(model, Vis, false,true);
                DrawCells(model, Vis2, false, false);
            }


            //System.out.println(model.GetPop());

        }

        win.Dispose();


    }






    public static void DrawCells(PramlintideResistanceModel model, GuiGridVis visCells, boolean drug, boolean drawRes) {
        for (int i = 0; i < visCells.length; i++) {
            Cell c=model.GetAgent(i);
            if(c==null){
                if (drug) {
                    visCells.SetPix(i, CategorialColor(4));
                } else {
                    visCells.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
                }

            } else{
                if (!drawRes) {
                    visCells.SetPix(i, RGB((double) 1, c.phenotype, (double) 0));
                } else {
                    if (c.res) {
                    visCells.SetPix(i, RGB((double) 0, 1, (double) 0));
                    } else {
                        visCells.SetPix(i, RGB((double) 1, 0, (double) 0));
                    }
                }


            }
        }
    }




    public static Grid2D readCSV(String filename, Grid2D myGrid) {
        String line = "";
        String cvsSplitBy = ",";

        Grid2D CSVGrid = new Grid2D(myGrid.xDim,myGrid.yDim);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            int i = 0;
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] myLine = line.split(cvsSplitBy);

                for (int j = 0; j < myGrid.yDim; j++) {
                    CSVGrid.Set(i,j,Double.parseDouble(myLine[j]));
                }

                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return CSVGrid;
    }
    public static void printGrid2D(Grid2D myGrid) {
        double[] row = new double[myGrid.yDim];
        for (int i = 0; i < myGrid.xDim; i++) {
            for (int j = 0; j < myGrid.yDim; j++) {
                row[j] = myGrid.Get(i,j);
            }
            System.out.println(ArrToString(row,","));
        }
    }
}


