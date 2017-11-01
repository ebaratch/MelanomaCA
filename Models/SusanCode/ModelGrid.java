package Models.SusanCode;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Models.SusanCode.CellTypes.*;
import static Framework.Utils.*;


class CellTypes {

    final static int FIBROBLASTi = 0; // inactive FRC cells

    final static int TCELLi = 1; // inactive T-cells

    final static int DCELLi = 2; // inactive dendritic cells

    final static int FIBROBLASTAPCM = 3; // active FRC cells, T-cells, CCL 19/21

    final static int TCELLa = 4; // active T-cells

    final static int DCELLf = 5; // APCF

    final static int TUMORCELL = 6; // tumor cell

    final static int BCELLi = 7; // inactive B-cells

    final static int BCELLa = 8; // active B-cells

    final static int FIBROBLASTT = 9; // FRC(T) cells

    final static int FIBROBLASTAPCOFF = 10; // FRC(APCoff) cells

    final static int FIBROBLASTAPCF = 11; // FRC(APCf) cells, B-cells, CXCL 13

    final static int FIBROBLASTB = 12; // FRC(B) cells

    final static int DCELLm = 13; // APCM

    final static int TCELLpi = 14; // permanently inactive T-cells

    final static int N_CELL_TYPES = 15;

    final static int RIGHT = 0;

    final static int LEFT = 1;

    final static int UP = 2;

    final static int DOWN = 3;

    final static int POS = 4;

    final static int COLOR_BLACK = RGB(0, 0, 0);

    final static int[] CellColors = new int[]{
            RGB(0.5, 0, 0.5),
            RGB(0, 1, 0),
            RGB(0.5, 0, 0.5),
            RGB(0.5, 0.5, 0),
            RGB(1, 0, 0),
            RGB(0, 0.3, 0),
            RGB(0.3, 0.3, 0.3),
            RGB(0, 0.5, 0.5),
            RGB(0, 0, 1),
            RGB(1, 1, 1),
            RGB(1, 1, 1),
            RGB(0.2, 0.8, 0),
            RGB(1, 1, 1),
            RGB(0, 0.3, 0),
            RGB(0.3, 0.3, 0),
    };
    final static int[] ActivationColors = new int[]{
            RGB(0, 1, 0),//Inactive
            RGB(1, 0, 0),//Active
            RGB(0, 0, 1)//other
    };
}

// had to turn up the size of the FRC cells so they don't proliferate out of control
public class ModelGrid extends AgentGrid2D<Cells> {

    public static boolean isOn = true;
    final GuiGridVis visCells;
    final GuiGridVis visFRCTumor;
    final GuiGridVis visT;
    final GuiGridVis visB;
    final GuiGridVis visD;
    final GuiGridVis visDiffCCL;
    final GuiGridVis visDiffCXCL;
    final GuiGridVis visDiffTumor;
    final int[] bigHood = Utils.MooreHood(false);
    final int[] smallHood = Utils.VonNeumannHood(false);
    // simulation constants
    public int pixelSize = 3;
    public int timeStep = 1; // 1 timestep = 4 minutes
    public int time = 2880 / timeStep; // 8 days
    public int tickRateTime = 1;
    public int days;
    public int hours;
    public int minutes;
    public int tempTime;
    // cell counts & chemokine gradient averages
    public int tumorCellCount = 0;
    public int tCellICount = 0;
    public int tCellACount = 0;
    public int bCellICount = 0;
    public int bCellACount = 0;
    public int dCellICount = 0;
    public int dCellMCount = 0;
    public int dCellFCount = 0;
    public int tCELLPICount = 0;
    public int FRCiCount = 0;
    public int FRCAPCfCount = 0;
    public int FRCAPCmCount = 0;
    public double tumorChemoAvg = 0;
    public double CCL1921Avg = 0;
    public double CXCL13Avg = 0;
    // cell activation & movement constants
    public int[][] carrCap = new int[this.xDim][this.yDim];
    public int carryingCap = 50; // max size/space available for cells to occupy at once
    public double levyProb = 1 - Math.pow(0.9, timeStep); // probability a cell that's unaffected by a chemokine gradient begins a levy walk
    public int cellDist = 2; // max. distance required for the activation of cells, 40 microns
    // tumor & frc constants
    public int numFRC; // number of FRC cells
    //public int numFRC = 150 + (int) (Math.random() * 150); // number of FRC cells
    public int numTumor = 40; // number of tumor cells
    public int patchSize; // radius of the circle where the FRC cells can appear
    public int tumorPatchSize = 5; // radius of the circle where the tumor cells can appear
    public boolean tumorDead = false;
    public boolean first = false; // checks the first timestep where the tumor is dead
    public boolean print = false;
    public double chemProd;
    public double chemDegTumor;
    public double chemDegFRC;
    public double chemConsumption;
    public double chemThreshold;
    public int tumorDeathTime;
    public boolean FRCAPCf = false;
    public boolean firstFRCAPCf = false;
    public int FRCAPCfDeathTime;
    public boolean FRCAPCm = false;
    public boolean firstFRCAPCm = false;
    public int FRCAPCmDeathTime;
    public boolean one = false;
    public boolean two = false;
    public boolean three = false;
    public boolean four = false;
    public int numOfPatches;
    // motile cell constants
    public int numT = 10; // number of starting T-cells
    public int numTp = 10; // number of starting permanently off T-cells
    //int numTp = 0; // number of starting permanently off T-cells
    public int numDC = 10; // number of starting D-cells
    public int numB = 10; // number of starting B-cells
    public int numNewCells; // number of new cells that can be created per timestep
    //    public double newTCellProb = 1 - Math.pow(0.5, timeStep); // probability that a new T-cell appears where the double is the prob that a new T-cell doesn't appear
    public double newTCellProb = 1 - Math.pow(0.5, timeStep); // probability that a new T-cell appears where the double is the prob that a new T-cell doesn't appear
    public double newTPCellProb = 1 - Math.pow(0.5, timeStep); // probability that a new permanently inactive T-cell appears
    public double newDCellProb = 1 - Math.pow(0.8, timeStep); // probability that a new D-cell appears
    public double newBCellProb = 1 - Math.pow(0.5, timeStep); // probability that a new B-cell appears
    public int TCellConstant = 1;
    public int TSize = 10; // size of a T-Cell
    public int TPSize = 10; // size of a permanently inactive T-cell
    public int DSize = 10; // size of a D-Cell
    public int BSize = 10; // size of a B-cell
    PDEGrid2D tumorChemo; // tumor chemokine
    PDEGrid2D frcCCL1921; // FRC CCL 19/21
    PDEGrid2D frcCXCL13; // FRC CXCL13
    PDEGrid2D motileChemo; // motile cell chemokine gradient
    Random rn;
    FileIO outputFileTumor; // file generated with the tumor size at each time step
    FileIO outputFileTCELLa; // file generated with the number of active T at each time step
    FileIO outputFileTCELLi; // file generated with the number of inactive T at each time step
    FileIO outputFileTCELLpi; // file generated with the number of permanently inactive T at each time step
    FileIO outputFileBCELLi; // file generated with the number of inactive B at each time step
    FileIO outputFileBCELLa; // file generated with the number of active B at each time step
    FileIO outputFileDCELLi; // file generated with the number of inactive D at each time step
    FileIO outputFileDCELLf; // file generated with the number of active DendF at each time step
    FileIO outputFileDCELLm; // file generated with the number of active DendM at each time step
    FileIO outputFileFRCi; // file generated with the number of inactive FRC at each time step
    FileIO outputFileFRCAPCf; // file generated with the number of FRF(APCf) at each time step
    FileIO outputFileFRCAPCm; // file generated with the number of FRF(APCm) at each time step
    FileIO outputFileTumorChemo; // file generated with average tumor chemokine concentrations at each time step
    FileIO outputFileCCL1921; // file generated with the average frc ccl 19/21 chemokine concentrations at each time step
    FileIO outputFileCXCL13; // file generated with the average frc cxcl 13 chemokine concentrations at each time step
    int[] divIs = new int[smallHood.length / 2]; // coordinates (Is) of the spaces that the tumor cells could previously divide into
    int [] divIsBig = new int[bigHood.length/2]; //coordinates (Is) of the spaces that the tumor cells can divide into

    ArrayList<Cells> temp = new ArrayList<Cells>(); // holds the position of cells at a certain location on the typeGrid

    /**
     * @param xDim x-dimensions of the typeGrid
     * @param yDim y-dimensions of the typeGrid
     */
    // -- here constructor
    public ModelGrid(
            int xDim,
            int yDim, int numFRC, int numNewCells, double chemProd, double chemConsumption, double chemDegTumor, double chemDegFRC, double chemThreshold, int patchSize, int numOfPatches,
            GuiLabel InactiveTCellLabel, GuiLabel PInactiveTCellLabel, GuiLabel ActiveTCellLabel,
            GuiLabel InactiveBCellLabel, GuiLabel ActiveBCellLabel,
            GuiLabel InactiveDCellLabel, GuiLabel DendFCellLabel,
            GuiLabel DendMCellLabel, GuiLabel TumorCellLabel, GuiLabel FRCiLabel, GuiLabel FRCAPCfLabel, GuiLabel FRCAPCmLabel, GuiLabel TimerLabel) {
        super(xDim, yDim, Cells.class);
        rn = new Random();
        frcCCL1921 = new PDEGrid2D(xDim, yDim);
        frcCXCL13 = new PDEGrid2D(xDim, yDim);
        tumorChemo = new PDEGrid2D(xDim, yDim);
        motileChemo = new PDEGrid2D(xDim, yDim);
        visCells = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui
        // with
        // the
        // cells
        visDiffCCL = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with CCL 19/21 diffusion
        //GuiGridVis visMotileDiff = new GuiGridVis( xDim, yDim, pixelSize ); // gui with the
        // diffusion according
        // to the motile cells
        visDiffCXCL = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the CXCL 13 diffusion
        visDiffTumor = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the tumor chemokine diffusion
        visT = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the T cells
        visFRCTumor = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the FRC and Tumor cells

        visB = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the B cells
        visD = new GuiGridVis(xDim, yDim, pixelSize, 1, 21, true); // gui with the D cells

        this.numFRC = numFRC;
        this.numNewCells = numNewCells;
        this.chemProd = chemProd;
        this.chemConsumption = chemConsumption;
        this.chemDegTumor = chemDegTumor;
        this.chemDegFRC = chemDegFRC;
        this.chemThreshold = chemThreshold;
        this.patchSize = patchSize;
        this.numOfPatches = numOfPatches;

        outputFileTumor = new FileIO("TumorSize.csv", "w"); // file with the tumor size over time
        outputFileTumor.Write("NumCellsInTumor\n");
        outputFileTCELLi = new FileIO("TCELLi.csv", "w"); // file with the number of TCELLi over time
        outputFileTCELLi.Write("NumTCELLi\n");
        outputFileTCELLa = new FileIO("TCELLa.csv", "w"); // file with the number of TCELLa over time
        outputFileTCELLa.Write("NumTCELLa\n");
        outputFileTCELLpi = new FileIO("TCELLpi.csv", "w"); // file with the number of TCELLpi over time
        outputFileTCELLpi.Write("NumTCELLpi\n");
        outputFileBCELLi = new FileIO("BCELLi.csv", "w"); // file with the number of BCELLi over time
        outputFileBCELLi.Write("NumBCELLi\n");
        outputFileBCELLa = new FileIO("BCELLa.csv", "w"); // file with the number of BCELLa over time
        outputFileBCELLa.Write("NumBCELLa\n");
        outputFileDCELLi = new FileIO("DCELLi.csv", "w"); // file with the number of DCELLi over time
        outputFileDCELLi.Write("NumDCELLi\n");
        outputFileDCELLf = new FileIO("DCELLf.csv", "w"); // file with the number of DCELLf over time
        outputFileDCELLf.Write("NumDCELLf\n");
        outputFileDCELLm = new FileIO("DCELLm.csv", "w"); // file with the number of DCELLm over time
        outputFileDCELLm.Write("NumDCELLm\n");
        outputFileFRCi = new FileIO("NumFRCi.csv", "w"); // file with the number of FRCi over time
        outputFileFRCi.Write("NumFRCi\n");
        outputFileFRCAPCf = new FileIO("NumFRCAPCf.csv", "w"); // file with the number of FRCAPCf over time
        outputFileFRCi.Write("NumFRCAPCf\n");
        outputFileFRCAPCm = new FileIO("NumFRCAPCm.csv", "w"); // file with the number of FRC over time
        outputFileFRCi.Write("NumFRCAPCm\n");
        outputFileTumorChemo = new FileIO("TumorChemo.csv", "w"); // file with the average tumor chemokine concentration over time
        outputFileTumorChemo.Write("TumorChemo\n");
        outputFileCCL1921 = new FileIO("CCL1921.csv", "w"); // file with the average CCL 19/21 concentration over time
        outputFileCCL1921.Write("CCL1921\n");
        outputFileCXCL13 = new FileIO("CXCL13.csv", "w"); // file with the average CXCL 13 concentration over time
        outputFileCXCL13.Write("CXCL13\n");
        //outputFileTLSResults = new FileIO("TLSResults" + args[0] + ".csv", "w"); // file with all of the run results
        //FileIO outputFileTLSResults = TLSResults;

//        if (this.numOfPatches == 1) {
//            one = true;
//        } else if (this.numOfPatches == 2) {
//            one = true;
//            two = true;
//            this.numFRC = this.numFRC/2;
//        } else if (this.numOfPatches == 3) {
//            one = true;
//            two = true;
//            three = true;
//            this.numFRC = this.numFRC/3;
//        } else if (this.numOfPatches == 4) {
//            one = true;
//            two = true;
//            three = true;
//            four = true;
//            this.numFRC = this.numFRC/4;
//        }

        for (int i = 0; i < numTumor; i++) {
            int xStore = rn.nextInt(tumorPatchSize);
            int x;
            int y;
            if (Math.random() < 0.5) {
                x = xStore + 2 * xDim / 3 + 1 - tumorPatchSize / 2;
//                x = xStore + xDim / 2 - tumorPatchSize / 2;
            } else {
                x = -xStore + 2 * xDim / 3 + 1 - tumorPatchSize / 2;
//                x = -xStore + xDim / 2 - tumorPatchSize / 2;
            }
            if (Math.random() < 0.5) {
                y = rn.nextInt((int) (Math.pow(Math.pow(tumorPatchSize, 2) - Math.pow(xStore, 2), 0.5)))
                + 2 * yDim / 3 + 1 - tumorPatchSize / 2;
//                y = rn.nextInt((int) (Math.pow(Math.pow(tumorPatchSize, 2) - Math.pow(xStore, 2), 0.5)))
//                        + yDim / 2 - tumorPatchSize / 2;

            } else {
                y = -1 * rn.nextInt((int) (Math.pow(Math.pow(tumorPatchSize, 2) - Math.pow(xStore, 2), 0.5)))
                        + 2 * yDim / 3 + 1 - tumorPatchSize / 2;
//                y = -1 * rn.nextInt((int) (Math.pow(Math.pow(tumorPatchSize, 2) - Math.pow(xStore, 2), 0.5)))
//                        + yDim / 2 - tumorPatchSize / 2;
            }
            ArrayList<Cells> temp = new ArrayList<Cells>();
            this.GetAgents(temp, x, y);
            if (temp.isEmpty()) {
                NewCell(x, y, TUMORCELL, POS, false, (int) (Math.random() * 361) / this.timeStep, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
                //NewCell(x, y, TUMORCELL, POS, false, 1000, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0,0); // impact of tdCount now??
            } else {
                i--;
            }
        }

                for (int i = 0; i < numFRC; i++) // creating the frc patch with inactive and active drawing in both T and B cells
                {
                    int xStore = rn.nextInt(patchSize);
                    int x;
                    int y;

//                    if (Math.random() < 0.5) {
//                x = xStore + 7 * xDim / 8 + 1 - patchSize / 2; // FRC patch is on the upper righthand corner
////                        x = xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                    } else {
                x = -xStore + xDim + 1 - patchSize / 2;
//                        x = -xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                    }
//                    if (Math.random() < 0.5) {
//                y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 7 * yDim / 8
//                        + 1 - patchSize / 2; // upper righthand
////                        y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
////                                + 1 - patchSize / 2; // middle
//                    } else {
                y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim + 1 - patchSize / 2; // upper righthand
//                        y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
//                                + 1 - patchSize / 2; // middle
//                    }
                    NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
                }

//        if (one) {
//            for (
//                    int i = 0;
//                    i < numFRC; i++) // creating the frc patch with inactive and active drawing in both T and B cells
//
//            {
//                int xStore = rn.nextInt(patchSize);
//                int x;
//                int y;
//
//                if (Math.random() < 0.5) {
////                x = xStore + 2 * xDim / 3 + 1 - patchSize / 2; // FRC patch is on the upper righthand corner
//                    x = xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                } else {
////                x = -xStore + 2 * xDim / 3 + 1 - patchSize / 2;
//                    x = -xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                }
//                if (Math.random() < 0.5) {
////                y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
//                            + 1 - patchSize / 2; // middle
//                } else {
////                y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
//                            + 1 - patchSize / 2; // middle
//                }
//                NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
////        }
//            }
//        }
//
//        if (two) {
//            for (int i = 0; i < numFRC; i++) // creating the frc patch with inactive and active drawing in both T and B cells
//            {
//                int xStore = rn.nextInt(patchSize);
//                int x;
//                int y;
//
//                if (Math.random() < 0.5) {
////                x = xStore + 2 * xDim / 3 + 1 - patchSize / 2; // FRC patch is on the upper righthand corner
//                    x = xStore + 3 * xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                } else {
////                x = -xStore + 2 * xDim / 3 + 1 - patchSize / 2;
//                    x = -xStore + 3 * xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                }
//                if (Math.random() < 0.5) {
////                y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 3 * yDim / 4
//                            + 1 - patchSize / 2; // middle
//                } else {
////                y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 3 * yDim / 4
//                            + 1 - patchSize / 2; // middle
//                }
//                NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
////        }
//            }
//        }
//
//        if (three) {
//            for (int i = 0; i < numFRC; i++) // creating the frc patch with inactive and active drawing in both T and B cells
//            {
//                int xStore = rn.nextInt(patchSize);
//                int x;
//                int y;
//
//                if (Math.random() < 0.5) {
////                x = xStore + 2 * xDim / 3 + 1 - patchSize / 2; // FRC patch is on the upper righthand corner
//                    x = xStore + 3 * xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                } else {
////                x = -xStore + 2 * xDim / 3 + 1 - patchSize / 2;
//                    x = -xStore + 3 * xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                }
//                if (Math.random() < 0.5) {
////                y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
//                            + 1 - patchSize / 2; // middle
//                } else {
////                y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + yDim / 4
//                            + 1 - patchSize / 2; // middle
//                }
//                NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
////        }
//            }
//        }
//
//        if (four) {
//            for (int i = 0; i < numFRC; i++) // creating the frc patch with inactive and active drawing in both T and B cells
//            {
//                int xStore = rn.nextInt(patchSize);
//                int x;
//                int y;
//
//                if (Math.random() < 0.5) {
////                x = xStore + 2 * xDim / 3 + 1 - patchSize / 2; // FRC patch is on the upper righthand corner
//                    x = xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                } else {
////                x = -xStore + 2 * xDim / 3 + 1 - patchSize / 2;
//                    x = -xStore + xDim / 4 + 1 - patchSize / 2; // FRC patch in the middle
//                }
//                if (Math.random() < 0.5) {
////                y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 3 * yDim / 4
//                            + 1 - patchSize / 2; // middle
//                } else {
////                y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 2 * yDim / 3
////                        + 1 - patchSize / 2; // upper righthand
//                    y = -rn.nextInt((int) (Math.pow(Math.pow(patchSize, 2) - Math.pow(xStore, 2), 0.5))) + 3 * yDim / 4
//                            + 1 - patchSize / 2; // middle
//                }
//                NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
////        }
//            }
//        }

//        for (int j = 0; j < numFRC; j++) // initially placing the FRC-cells in the
//        // window
//        {
//            int x = rn.nextInt(xDim);
//            int y = rn.nextInt(yDim);
//            NewCell(x, y, FIBROBLASTi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
//
//        }

        for (int j = 0; j < numT; j++) // initially placing the T-cells in the
        // window
        {
            int x = rn.nextInt(xDim);
            int y = rn.nextInt(yDim);
            NewCell(x, y, TCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);

        }

        for (int t = 0; t < numTp; t++) // initially placing the permanently off T-cells in the window
        {
            int x = rn.nextInt(xDim);
            int y = rn.nextInt(yDim);
            NewCell(x, y, TCELLpi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
        }

        for (int k = 0; k < numDC; k++) // initially placing the D-cells in
        // the window
        {
            int x = rn.nextInt(xDim);
            int y = rn.nextInt(yDim);
            NewCell(x, y, DCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
        }
        for (int k = 0; k < numB; k++) {
            int x = rn.nextInt(xDim);
            int y = rn.nextInt(yDim);
            NewCell(x, y, BCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(timeStep), false, 0, 0, 0);
        }
    }

    public static void main(String[] args) {
        TickTimer trt = new TickTimer();
//        FileIO outputFileTLSResults;
//        if (ModelGrid.isOn) {
//            outputFileTLSResults = new FileIO("TLSResults.csv", "w");
//        } else {
//            outputFileTLSResults = new FileIO("TLSResults" + args[0] + ".csv", "w");
//            outputFileTLSResults.Write("TumorCellCount, TCELLiCount, TCELLPiCount, TCELLaCount, BCELLiCount, BCELLaCount, DCELLiCount, DCELLfCount, DCELLmCount, FRCiCount, FRCAPCfCount, FRCAPCmCount, TumorChemoAvg, CCL1921Avg, CXCL13Avg\n");
//        }

        Random rn = new Random();
        GuiWindow gui = new GuiWindow("StaticModel", true, (e) -> {
            System.out.println("done!");
        }, ModelGrid.isOn);// third part = the shorthand implementation of GuiCloseAction/what
        // you want to occur after the simulation is done running!

        GuiLabel InactiveTCellLbl = new GuiLabel("Inactive T Cells:");
        GuiLabel PInactiveTCellLbl = new GuiLabel("Permanently Inactive T Cells: ");
        GuiLabel ActiveTCellLbl = new GuiLabel("Active T Cells:");
        GuiLabel InactiveBCellLbl = new GuiLabel("Inactive B Cells:");
        GuiLabel ActiveBCellLbl = new GuiLabel("Active B Cells:");
        GuiLabel InactiveDCellLbl = new GuiLabel("Inactive D Cells:");
        GuiLabel DendMCellLbl = new GuiLabel("Dend M Cells:");
        GuiLabel DendFCellLbl = new GuiLabel("Dend F Cells:");
        GuiLabel TumorCellLbl = new GuiLabel("Tumor Cells:");
        GuiLabel FRCiCellLbl = new GuiLabel("FRCi Cells:");
        GuiLabel FRCAPCfCellLbl = new GuiLabel("FRCAPCf Cells:");
        GuiLabel FRCAPCmCellLbl = new GuiLabel("FRCAPCm Cells:");
        GuiLabel TimerLbl = new GuiLabel("Time: ");

        ModelGrid model = new ModelGrid(120,
                120, 100, 1, 0.1, .2, 0.002, 0.01, 1, 13, 1,
                InactiveTCellLbl, PInactiveTCellLbl,
                ActiveTCellLbl,
                InactiveBCellLbl,
                ActiveBCellLbl,
                InactiveDCellLbl,
                DendFCellLbl,
                DendMCellLbl, TumorCellLbl, FRCiCellLbl, FRCAPCfCellLbl, FRCAPCmCellLbl, TimerLbl
        );

        gui.AddCol(0, new GuiLabel("T-cells"));
        gui.AddCol(0, model.visT);// Adds a "viewing window" from the top

        gui.AddCol(1, new GuiLabel("B-cells"));
        gui.AddCol(1, model.visB);// Adds a "viewing window" from the top

        gui.AddCol(2, new GuiLabel("CCL 19/21 chemokine"));
        gui.AddCol(2, model.visDiffCCL);// Adds a "viewing window" from the top

        gui.AddCol(4, new GuiLabel("CXCL 13 chemokine"));
        gui.AddCol(4, model.visDiffCXCL);// Adds a "viewing window" from the top

        gui.AddCol(0, new GuiLabel("APC cells"));
        gui.AddCol(0, model.visD);// Adds a "viewing window" from the top

        gui.AddCol(1, new GuiLabel("FRC-cells"));
        gui.AddCol(1, model.visFRCTumor);// Adds a "viewing window" from the top

        gui.AddCol(2, new GuiLabel("Tumor chemokine"));
        gui.AddCol(2, model.visDiffTumor);// Adds a "viewing window" from the top

        gui.AddCol(3, new GuiLabel("All Cells"));
        gui.AddCol(3, model.visCells);// Adds a "viewing window" from the top

        gui.AddCol(3, new GuiLabel("Cell Types & Colors for All Cells"));
        gui.AddCol(3, new GuiLabel(" "));
        gui.AddCol(3, new GuiLabel("FRC: i = magenta, APCM = yellow, APCF = bright green"));
        gui.AddCol(3, new GuiLabel("T-Cell: inactive = green, Pi = dull yellow, active = red"));
        gui.AddCol(3, new GuiLabel("B-Cell: inactive = teal, active = blue"));
        gui.AddCol(3, new GuiLabel("APCi = magenta, APCF and APCM = dark green"));
        gui.AddCol(3, new GuiLabel(" "));
        gui.AddCol(3, InactiveTCellLbl);
        gui.AddCol(3, PInactiveTCellLbl);
        gui.AddCol(3, ActiveTCellLbl);
        gui.AddCol(3, InactiveBCellLbl);
        gui.AddCol(3, ActiveBCellLbl);
        gui.AddCol(3, InactiveDCellLbl);
        gui.AddCol(3, DendMCellLbl);
        gui.AddCol(3, DendFCellLbl);
        gui.AddCol(3, TumorCellLbl);
        gui.AddCol(3, FRCiCellLbl);
        gui.AddCol(3, FRCAPCfCellLbl);
        gui.AddCol(3, FRCAPCmCellLbl);
        gui.AddCol(3, new GuiLabel(" "));
        gui.AddCol(3, TimerLbl);

        gui.RunGui();

        for (int i = 0; i < model.time; i++) // one timestep of the simulation
        {
            trt.TickPause(model.tickRateTime); // smaller number = faster simulation
            model.tempTime = 4 * i * model.timeStep; // displaying the time
            model.days = model.tempTime / 1440;
            model.hours = model.tempTime % 1440 / 60;
            model.minutes = model.tempTime % 1440 % 60;
            model.Step();


            if (isOn) {
                model.outputFileTumor.Write(model.tumorCellCount + " \n");
                model.outputFileTCELLi.Write(model.tCellICount + " \n");
                model.outputFileTCELLa.Write(model.tCellACount + " \n");
                model.outputFileTCELLpi.Write(model.tCELLPICount + " \n");
                model.outputFileBCELLi.Write(model.bCellICount + " \n");
                model.outputFileBCELLa.Write(model.bCellACount + " \n");
                model.outputFileDCELLi.Write(model.dCellICount + " \n");
                model.outputFileDCELLf.Write(model.dCellFCount + " \n");
                model.outputFileDCELLm.Write(model.dCellMCount + " \n");
                model.outputFileFRCi.Write(model.FRCiCount + " \n");
                model.outputFileFRCAPCf.Write(model.FRCAPCfCount + " \n");
                model.outputFileFRCAPCm.Write(model.FRCAPCmCount + " \n");
                model.outputFileTumorChemo.Write(model.tumorChemoAvg + " \n");
                model.outputFileCCL1921.Write(model.CCL1921Avg + " \n");
                model.outputFileCXCL13.Write(model.CXCL13Avg + " \n");
            }
//            else {
//                outputFileTLSResults.Write(model.tumorCellCount + "," + model.tCellICount + "," + model.tCELLPICount + "," + model.tCellACount + "," + model.bCellICount + "," + model.bCellACount + "," + model.dCellICount + "," + model.dCellFCount + "," + model.dCellMCount + "," + model.FRCiCount + "," + model.FRCAPCfCount + "," + model.FRCAPCmCount + "," + model.tumorChemoAvg + "," + model.CCL1921Avg + "," + model.CXCL13Avg + "," + "\n"); // TODO *** if this works, add in the other cell types & values
//
//            }

            InactiveTCellLbl.SetText("Inactive T Count: " + model.tCellICount);
            PInactiveTCellLbl.SetText("Permanently Inactive T Count: " + model.tCELLPICount);
            ActiveTCellLbl.SetText("Active T Count: " + model.tCellACount);
            InactiveBCellLbl.SetText("Inactive B Count: " + model.bCellICount);
            ActiveBCellLbl.SetText("Active B Count: " + model.bCellACount);
            InactiveDCellLbl.SetText("Inactive D Count: " + model.dCellICount);
            DendFCellLbl.SetText("DendF Count: " + model.dCellFCount);
            DendMCellLbl.SetText("DendM Count: " + model.dCellMCount);
            TumorCellLbl.SetText("Tumor Cell Count: " + model.tumorCellCount);
            FRCiCellLbl.SetText("FRCi Cell Count: " + model.FRCiCount);
            FRCAPCfCellLbl.SetText("FRCAPCf Cell Count: " + model.FRCAPCfCount);
            FRCAPCmCellLbl.SetText("FRCAPCm Cell Count: " + model.FRCAPCmCount);
            TimerLbl.SetText(model.days + " Days " + model.hours + " Hours " + model.minutes + " Minutes ");


        }
        model.outputFileTumor.Close();
        model.outputFileTCELLi.Close();
        model.outputFileTCELLa.Close();
        model.outputFileTCELLpi.Close();
        model.outputFileBCELLi.Close();
        model.outputFileBCELLa.Close();
        model.outputFileDCELLi.Close();
        model.outputFileDCELLf.Close();
        model.outputFileDCELLm.Close();
        model.outputFileFRCi.Close();
        model.outputFileFRCAPCf.Close();
        model.outputFileFRCAPCm.Close();
        model.outputFileTumorChemo.Close();
        model.outputFileCCL1921.Close();
        model.outputFileCXCL13.Close();
//        outputFileTLSResults.Close();

        gui.Dispose();
    }

    Cells NewCell(int x, int y, int cellType, int cellDirection, boolean isLevy, int count, boolean start, int TCK, int numDivis, int tdCount, int killTime, boolean tStart, double curr, int tKillTime, int prolif) // cell
    // cell constructor
    {
        Cells c = NewAgentSQ(x, y);
        c.Init(cellType, cellDirection, isLevy, count, start, TCK, numDivis, tdCount, killTime, tStart, curr, tKillTime, prolif);
        return c;
    }

    void Step() {

        this.tumorCellCount = 0;
        this.tCellICount = 0;
        this.tCELLPICount = 0;
        this.tCellACount = 0;
        this.bCellICount = 0;
        this.bCellACount = 0;
        this.dCellICount = 0;
        this.dCellFCount = 0;
        this.dCellMCount = 0;
        this.FRCiCount = 0;
        this.FRCAPCfCount = 0;
        this.FRCAPCmCount = 0;
        this.tumorChemoAvg = 0;
        this.CXCL13Avg = 0;
        this.CCL1921Avg = 0;

        for (int r = 0; r < this.xDim; r++) {
            for (int c = 0; c < this.yDim; c++) {
                this.tumorChemoAvg += this.tumorChemo.Get(r, c) / (this.xDim * this.yDim);
                this.CCL1921Avg += this.frcCCL1921.Get(r, c) / (this.xDim * this.yDim);
                this.CXCL13Avg += this.frcCXCL13.Get(r, c) / (this.xDim * this.yDim);
            }
        }
        for (Cells a : this) // chemokine gradient emitted if the cell is
        // an active FRC cell
        {

            if (a.getType() == FIBROBLASTAPCM && a.getTDCount() > 0) {
                //if (a.getCurr() + chemProd * timeStep <= 1) {
                a.setCurr(a.getCurr() + chemProd * timeStep);
                //}
                this.frcCCL1921.Set(a.Xsq(), a.Ysq(), a.getCurr());
                a.setTDCount(a.getTDCount() - this.timeStep);
            } else if (a.getType() == FIBROBLASTAPCM && a.getTDCount() <= 0) // FRC deactivates
            {
                a.setType(FIBROBLASTi);
            }
            if (a.getType() == FIBROBLASTAPCF && a.getTDCount() > 0) {
                //if (a.getCurr() + chemProd * timeStep <= 1) {
                a.setCurr(a.getCurr() + chemProd * timeStep);
                //}
                this.frcCXCL13.Set(a.Xsq(), a.Ysq(), a.getCurr());
                a.setTDCount(a.getTDCount() - this.timeStep);
            } else if (a.getType() == FIBROBLASTAPCF && a.getTDCount() <= 0)// FRC deactivates
            {
                a.setType(FIBROBLASTi);
            }
            if (a.getType() == TUMORCELL) {
                //if (a.getCurr() + chemProd * timeStep <= 1) {
                a.setCurr(a.getCurr() + chemProd * timeStep);
                //}
                this.tumorChemo.Set(a.Xsq(), a.Ysq(), a.getCurr());
                this.tumorCellCount++;
                if (a.getCount() == 0) {
                    this.divide(a);
                } else {
                    a.setCount(a.getCount() - this.timeStep);
                }
            }
            if (a.getType() == BCELLi) {
                this.bCellICount++;
            } else if (a.getType() == BCELLa) {
                this.bCellACount++;
                //if (a.getCurr() + chemProd * timeStep <= 1) {
                a.setCurr(a.getCurr() + chemProd * timeStep);
                //}
                a.setProlif(a.getProlif() - this.timeStep);
                //model.frcCXCL13.SetColor( a.Xsq(), a.Ysq(), a.getCurr() ); // Active B-cells emit CXCL13
            } else if (a.getType() == TCELLi) {
                this.tCellICount++;
            } else if (a.getType() == TCELLpi) {
                this.tCELLPICount++;
            } else if (a.getType() == TCELLa) {
                if (a.getTCK() <= 0) // active T-cells die after killing a certain number of tumor cells
                {
                    a.Die();
                }
                a.setProlif(a.getProlif() - this.timeStep);
                this.tCellACount++;
            } else if (a.getType() == DCELLi) {
                this.dCellICount++;
            } else if (a.getType() == DCELLf) {
                this.dCellFCount++;
            } else if (a.getType() == DCELLm) {
                this.dCellMCount++;
            } else if (a.getType() == FIBROBLASTi) {
                this.FRCiCount++;
            } else if (a.getType() == FIBROBLASTAPCF) {
                this.FRCAPCfCount++;
                a.setProlif(a.getProlif() - this.timeStep);
            } else if (a.getType() == FIBROBLASTAPCM) {
                this.FRCAPCmCount++;
                a.setProlif(a.getProlif() - this.timeStep);
            }

            for (int h = 0; h < timeStep; h++) {
                if (a.getNumDivis() > 0 ) {
                    a.setNumDivis(a.getNumDivis() - 1);
                    this.divide(a);
                }
            }

            if (a.getType() != TUMORCELL && a.getType() != FIBROBLASTi && a.getType() != FIBROBLASTAPCF && a.getType() != FIBROBLASTAPCM && a.getKillTime() > 0) // fixed cells' kill time doesn't get decremented so they stay alive
            {
                //a.setKillTime(a.getKillTime() - this.timeStep);
            } else if (a.getKillTime() <= 0 && a.Alive()) {
                a.Die();
            }
        }
//        outputFileTLSResults = new FileIO("TLSResults" + args[0] + ".csv", "w");
//        outputFileTLSResults.Write("TumorCellCount, TCELLiCount, TCELLPiCount, TCELLaCount, BCELLiCount, BCELLaCount, DCELLiCount, DCELLfCount, DCELLmCount, FRCiCount, FRCAPCfCount, FRCAPCmCount, TumorChemoAvg, CCL1921Avg, CXCL13Avg\n");
        // here -- step
//        for (int l = 0; l < this.xDim; l++) // diffusion for the chemokine gradients
//        {
//            for (int j = 0; j < this.yDim; j++) {
//                this.visDiffCCL.SetColorHeat(l, j, (float) (this.frcCCL1921.GetPix(l, j)));
//                this.visDiffCXCL.SetColorHeat(l, j, (float) (this.frcCXCL13.GetPix(l, j)));
//                this.visDiffTumor.SetColorHeat(l, j, (float) (this.tumorChemo.GetPix(l, j)));
//                //visMotileDiff.SetColorHeat( l, j, (float)model.motileChemo.GetPix( l, j ) );
//            }
//        }
        visDiffCCL.DrawGridDiff(frcCCL1921,Utils::HeatMapGBR);
        visDiffCCL.DrawGridDiff(frcCXCL13,Utils::HeatMapBGR);
        visDiffCCL.DrawGridDiff(tumorChemo,Utils::HeatMapBRG);
//        visB.DrawAgents(this,(Cells c)->{
//            switch (c.getType()){
//                case BCELLa: return RGB(1,0,0);
//                case BCELLi: return RGB(0,1,0);
//                default: return RGB(0,0,0);
//            }
//        },0,0,0);

        newTCellProb = Math.tanh(this.tumorChemoAvg * TCellConstant);
        if (newTCellProb < 0.5) {
            newTCellProb = 0.5;
        }
        if (rn.nextDouble() < newTCellProb) // adds new T-cells
        {
            for (int w = 0; w < this.numNewCells; w++) {
                int x = rn.nextInt(this.xDim);
                int y = rn.nextInt(this.yDim);
                if (this.carrCap[x][y] + this.TSize <= this.carryingCap) {
                    Cells a = this.NewCell(x, y, TCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(this.timeStep), false, 0, 0, 0);
                    a.setSizeSpeed();
                }
            }
        }
        if (rn.nextDouble() < this.newTPCellProb) // adds new permanently inactive T-cells
        {
            for (int w = 0; w < this.numNewCells; w++) {
                int x = rn.nextInt(this.xDim);
                int y = rn.nextInt(this.yDim);
                if (this.carrCap[x][y] + this.TPSize <= this.carryingCap) {
                    Cells a = this.NewCell(x, y, TCELLpi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(this.timeStep), false, 0, 0, 0);
                    a.setSizeSpeed();
                }
            }
        }
        if (rn.nextDouble() < this.newDCellProb) // adds new D-cells
        {
            for (int w = 0; w < this.numNewCells; w++) {
                int x = rn.nextInt(this.xDim);
                int y = rn.nextInt(this.yDim);
                if (this.carrCap[x][y] + this.DSize <= this.carryingCap) {
                    Cells a;
//                    if (Math.random() < 0.5) {
                    a = this.NewCell(x, y, DCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(this.timeStep), false, 0, 0, 0);
//                    } else {
//                        a = this.NewCell(x, y, DCELLi, POS, false, 0, false, 0, 0, 1, this.getCellLifeSpan(this.timeStep), false, 0, 0,0);
//
//                    }
                    a.setSizeSpeed();
                }
            }
        }
        if (rn.nextDouble() < this.newBCellProb) // adds new B-cells
        {
            for (int w = 0; w < this.numNewCells; w++) {
                int x = rn.nextInt(this.xDim);
                int y = rn.nextInt(this.yDim);
                if (this.carrCap[x][y] + this.BSize <= this.carryingCap) {
                    Cells a = this.NewCell(x, y, BCELLi, POS, false, 0, false, 0, 0, 0, this.getCellLifeSpan(this.timeStep), false, 0, 0, 0);
                    a.setSizeSpeed();
                }
            }
        }
        this.frcCCL1921.Diffusion(0.25, 0.0, false, false); // swaps the old view for
        PDEGrid2D ccl = this.frcCCL1921;
        for (int e = 0; e < ccl.length; e++) {
            ccl.Set(e, ccl.Get(e) * getDegradationRate(chemDegFRC, timeStep));
        }
        // chemokine gradient
        this.frcCXCL13.Diffusion(0.25, 0.0, false, false); // swaps the old view for
        //Example of degradation
        PDEGrid2D cxcl = this.frcCXCL13;
        for (int j = 0; j < cxcl.length; j++) {
            cxcl.Set(j, cxcl.Get(j) * getDegradationRate(chemDegFRC, timeStep));
        }
        // the new view
        // continuously, FRC
        // chemokine gradient for B-cells
        this.tumorChemo.Diffusion(0.25, 0.0, false, false); // swaps the old view for
        // the new view
        PDEGrid2D tumorChemo = this.tumorChemo;                       // continuously, tumor chemokine gradient
        for (int e = 0; e < tumorChemo.length; e++) {
            tumorChemo.Set(e, tumorChemo.Get(e) * getDegradationRate(chemDegTumor, timeStep));
        }

        this.motileChemo.Diffusion(0.25, 0.0, false, false); // swaps the old view for
        // the new view
        // continuously, motile
        // cell chemokine gradient
        PDEGrid2D motileChemokine = this.motileChemo;
        for (int e = 0; e < motileChemokine.Get(e); e++) {
            motileChemokine.Set(e, motileChemokine.Get(e) * getDegradationRate(chemDegFRC, timeStep));
        }
        for (int z = 0; z < this.timeStep; z++) {

            for (Cells a : this) // cell movement/migration
            {
                int nearBorder = this.HoodToIs(this.smallHood, this.divIs, a.Xsq(), a.Ysq());
                if (nearBorder != divIs.length) {
                    a.Die();
                    continue;
                }
                boolean moving = a.CalcDirection();
                int speed = a.getSpeed();
                if (moving && speed != 0) {

                    for (int j = 0; j < speed; j++) {
                        int x = a.Xsq();
                        int y = a.Ysq();

                        if (j != 0) {
                            nearBorder = this.HoodToIs(this.smallHood, this.divIs, a.Xsq(), a.Ysq());
                            if (nearBorder != divIs.length) {
                                a.Die();
                                break;
                            }
                        }
                        int iMove = this.divIs[a.getDirection()];
                        int xMove = this.ItoX(iMove);
                        int yMove = this.ItoY(iMove);

                        if (this.carrCap[xMove][yMove] + a.getSize() <= this.carryingCap) {
                            a.MoveSQ(xMove, yMove);
                            a.drawMove(x, y);
                        }
                    }
                    if (!a.Alive()) {
                        continue;
                    }
                } else {
                    a.draw();
                }
                for (Cells c : this) // checks if the right cell types are
                // close to each other - if so, activate
                {

                    if (c.isClose(a, this.cellDist) && !c.equals(a)) {
                        c.activate(a);
                        if (c.getType() == TUMORCELL && a.getType() == TCELLa) {

                            if (a.getTStart() == false && !tumorDead) {
                                a.setSpeed(0);
                                a.setTKillTime(this.getTKillTime(this.timeStep));
                                a.setTStart(true);
                            } else if (a.getTStart() == true) {
                                a.setTKillTime(a.getTKillTime() - this.timeStep);
                            }
                            if (a.getTKillTime() <= 0 && a.getTStart() == true) {
                                if (a.Alive()) {
                                    a.setTCK(a.getTCK() - 1);
                                    c.Die();
                                    a.setTKillTime(this.getTKillTime(this.timeStep));
                                    a.setTStart(false);
                                    a.setSpeed(2);
                                }
                            }
                        }
                        /*
                        if (c.getType() == TUMORCELL && a.getType() == TCELLa) {
                            if (a.getTStart() == false && this.tumorCellCount > 0) {
                                a.setSpeed(0);
                                a.setTKillTime(this.getTKillTime(this.timeStep));
                                a.setTStart(true);
                            }
                            else if (a.getTKillTime() > 0 && a.getTStart() == true) {
                                a.setTKillTime(a.getTKillTime() - this.timeStep);
                            } else if (a.getTKillTime() <= 0 && a.getTStart() == true) {
                                if (a.Alive()) {
                                    a.setTCK(a.getTCK() - 1);
                                    c.Die();
                                    a.setTKillTime(this.getTKillTime(this.timeStep));
                                    a.setTStart(false);
                                    a.setSizeSpeed();
                                }
                            }
                        }
                        */
                    }

                }
            }
        }

        for (Cells a : this) {
            boolean nearTumor = false;
            for (Cells c : this) {
                if (c.isClose(a, this.cellDist) && c.getType() == TUMORCELL && c.Alive() && a.getType() == TCELLa && a.Alive()) {
                    nearTumor = true;
                }
            }
            if (nearTumor == false) {
                a.setSpeed(2);
            }
        }

        for (int k = 0; k < this.xDim; k++) // putting the correct carryingCapacity values into the array
        {
            for (int j = 0; j < this.yDim; j++) {
                this.carrCap[k][j] = 0;
                ArrayList<Cells> g = new ArrayList<Cells>();
                this.GetAgents(g, k, j);
                for (Cells c : g) {
                    if (c.getType() == TCELLa && c.getSpeed() == 0) {
                        c.setSize(10);
                    } else {
                        c.setSizeSpeed();
                    }
                    this.carrCap[k][j] += c.getSize();
                }

            }
        }

        if (this.tumorCellCount <= 0 && this.first) {
            this.tumorDead = true;
        }

        if (this.tumorCellCount <= 0) {
            first = true;
        }

        if (this.FRCAPCfCount > 0) {
            this.FRCAPCf = true;
        }

        if (this.FRCAPCmCount > 0) {
            this.FRCAPCm = true;
        }

        //            if (tumorDead)
        //            {
        if (this.tumorDead && !this.first) {
            this.first = true;
            this.print = true; // -- heree
            tumorDeathTime = 1440 * this.days + 60 * this.hours + this.minutes;
            System.out.println(this.days + " Days " + this.hours + " Hours " + this.minutes + " Minutes ");
            System.out.println("TCELLi: " + this.tCellICount + " TCELLpi: " + this.tCELLPICount + " TCELLa: " + this.tCellACount);
            System.out.println("BCELLi: " + this.bCellICount + " BCELLa: " + this.bCellACount);
            System.out.println("DCELLi: " + this.dCellICount + " DCELLf: " + this.dCellFCount + " DCELLm: " + this.dCellMCount);
            //System.out.print(" tumorChemo avg: " + this.tumorChemoAvg + " CCL 19/21 avg: " + this.CCL1921Avg + " CXCL13 avg: " + this.CXCL13Avg);

        }

        if (this.FRCAPCf && !this.firstFRCAPCf) {
            this.firstFRCAPCf = true;
            FRCAPCfCount = 1440 * this.days + 60 * this.hours + this.minutes;
        }

        if (this.firstFRCAPCm && !this.firstFRCAPCm) {
            this.firstFRCAPCm = true;
            FRCAPCmCount = 1440 * this.days + 60 * this.hours + this.minutes;
        }
        //                model.outputFileTumor.Close();
        //                model.outputFileTCELLi.Close();
        //                model.outputFileTCELLa.Close();
        //                model.outputFileTCELLpi.Close();
        //                model.outputFileBCELLi.Close();
        //                model.outputFileBCELLa.Close();
        //                model.outputFileDCELLi.Close();
        //                model.outputFileDCELLf.Close();
        //                model.outputFileDCELLm.Close();
        //                model.outputFileFRCi.Close();
        //                model.outputFileFRCAPCf.Close();
        //                model.outputFileFRCAPCm.Close();
        //                model.outputFileTumorChemo.Close();
        //                model.outputFileCCL1921.Close();
        //                model.outputFileCXCL13.Close();
        //
        //                gui.Dispose();
        //            }

//        this.tumorCellCount = 0;
//        this.tCellICount = 0;
//        this.tCELLPICount = 0;
//        this.tCellACount = 0;
//        this.bCellICount = 0;
//        this.bCellACount = 0;
//        this.dCellICount = 0;
//        this.dCellFCount = 0;
//        this.dCellMCount = 0;
//        this.FRCiCount = 0;
//        this.FRCAPCfCount = 0;
//        this.FRCAPCmCount = 0;
//        this.tumorChemoAvg = 0;
//        this.CXCL13Avg = 0;
//        this.CCL1921Avg = 0;

        this.CleanShuffInc(this.rn);

    }

//    int findMax(double up, double down, double left, double right, double pos) // finds the
//    // maximum
//    // chemokine
//    // diffusion
//    // concentration
//    {
//        if (up > down && up > left && up > right && up > pos) {
//            return UP;
//        } else if (down > up && down > left && down > right && down > pos) {
//            return DOWN;
//        } else if (left > up && left > down && left > right && left > pos) {
//            return LEFT;
//        } else if (right > up && right > down && right > left && right > pos) {
//            return RIGHT;
//        } else {
//            return POS;
//        }
//    }

    int randomDir() // picks a random cell direction with equal probability
    {
        double x = Math.random();
        if (x < .2) {
            return UP;
        } else if (x < .4) {
            return DOWN;
        } else if (x < .6) {
            return LEFT;
        } else if (x < .8) {
            return RIGHT;
        } else {
            return POS;
        }
    }


    double subtract(double x, double y) {
        return x - y;
    }


    double cellMagnitude(double x, double y) {
        return Math.pow(Math.pow(x, 2) + Math.pow(y, 2), 0.5);
    }


    int probMatrix(double vertProb, double horiProb) // moves off even if the cell is currently at
    // the highest chemokine value?
    {

        double x = Math.random();

        if (vertProb >= 0 && x < vertProb) {
            return UP;
        } else if (vertProb <= 0 && x < Math.abs(vertProb)) {
            return DOWN;
        } else if (horiProb >= 0) {
            return RIGHT;
        } else {
            return LEFT;
        }
    }


    double cellMapping(double x, double upperBound) {
        if (x < upperBound) // upperBound for the magnitude
        {
            return x / upperBound;
        } else {
            return 0.8; // maximum probability;
        }
    }


    int calculateDir(double mappingProb, double vertProb, double horiProb) {
        double y = Math.random();
        if (y < mappingProb) {
            return probMatrix(vertProb, horiProb);
        } else {
            return randomDir();
        }
    }

    Cells divide(Cells c) {
        // int [] mooreHood = {-1, 1, 0, 1, 1, 1, -1, 0, 0, 0, 1, 0, -1, -1, 0, -1, 1, -1};
        int nIs = HoodToIs(bigHood, divIsBig, c.Xsq(), c.Ysq(), false, false);
        int nDivIs = 0;
        for (int j = 0; j < nIs; j++) {

            // find open squares
            boolean isTumor = false;
            temp.clear();
            this.GetAgents(temp, divIsBig[j]);
            int carryingCaps = 0;

//            for (Cells a : temp) {
//                if (a.getType() == TUMORCELL && c.getType() == TUMORCELL) {
//                    isTumor = true;
//                }
//            }
            for (Cells a : temp) {
                carryingCaps += a.getSize();
            }
            if (carryingCaps + c.getSize() > this.carryingCap) {
                isTumor = true;
            }

            if (isTumor == false) // temp.isEmpty()
            {
                divIsBig[nDivIs] = divIsBig[j];
                nDivIs++;
            }
        }
        if (nDivIs > 0) {
            // create daughter cell if an empty square exists
            Cells a = null;
            int i = divIsBig[rn.nextInt(nDivIs)];
            if (c.getType() == TUMORCELL) {
                a = NewCell(this.ItoX(i), this.ItoY(i), c.getType(), POS, false, this.getTumorProlifRate(timeStep), false, 0, 0, 0, c.getKillTime(), false, 0, 0, 0);
                a.setSizeSpeed();
                a.draw();
            } else if (((c.getType() == TCELLa || c.getType() == BCELLa) && c.getProlif() <= 0) || c.getType() == FIBROBLASTAPCM || c.getType() == FIBROBLASTAPCF) {
                a = NewCell(this.ItoX(i), this.ItoY(i), c.getType(), POS, false, 0, false, 5, c.getNumDivis(), 0, this.getCellLifeSpan(timeStep), false, 0, 0, this.getProlif(timeStep));//how should the tdCount look?
                a.setSizeSpeed();
                a.draw();
            }
            return a;
        }
        return null;
    }

    int getCellLifeSpan(int timeStep) {
        return (150 + (int) (Math.random() * 16)) / timeStep;
    } // cells live for 10-11 hours

    int getTKillTime(int timeStep) {
        return (60 + (int) (Math.random() * 16)) / timeStep;
        //return 1000;
    } // 4-5 hrs

    int getTumorProlifRate(int timeStep) {

        return (270 + (int) (Math.random() * 91)) / timeStep; // 18-24 hours
    }

    void consumeCCL(int x, int y, double amount, int timeStep) {
        if (this.frcCCL1921.Get(x, y) >= amount) {
            this.frcCCL1921.Set(x, y, this.frcCCL1921.Get(x, y) - amount * timeStep);
        } else {
            this.frcCCL1921.Set(x, y, 0);
        }
    }

    void consumeCXCL(int x, int y, double amount, int timeStep) {
        if (this.frcCXCL13.Get(x, y) >= amount) {
            this.frcCXCL13.Set(x, y, this.frcCXCL13.Get(x, y) - amount * timeStep);
        } else {
            this.frcCXCL13.Set(x, y, 0);
        }
    }

    void consumeTumorGrad(int x, int y, double amount, int timeStep) {
        if (this.tumorChemo.Get(x, y) >= amount) {
            this.tumorChemo.Set(x, y, this.tumorChemo.Get(x, y) - amount * timeStep);
        } else {
            this.tumorChemo.Set(x, y, 0);
        }
    }

    double getDegradationRate(double amount, int timeStep) {
        return 1 - Math.pow(amount, timeStep);
    }

    int getProlif(int timeStep) {
        return (150 + (int) (Math.random() * 31)) / timeStep; // active t-cells take 10-12 hours to proliferate
    }

    public int getTumorCellCount() {
        return tumorCellCount;
    }

    public int gettCELLICount() {
        return tCellICount;
    }

    public int gettCELLPICount() {
        return tCELLPICount;
    }

    public int gettCELLACount() {
        return tCellACount;
    }

    public int getbCELLICount() {
        return bCellICount;
    }

    public int getbCELLACount() {
        return bCellACount;
    }

    public int getdCELLICount() {
        return dCellICount;
    }

    public int getdCellFCount() {
        return dCellFCount;
    }

    public int getdCellMCount() {
        return dCellMCount;
    }

    public int getFRCiCount() {
        return FRCiCount;
    }

    public int getFRCAPCfCount() {
        return FRCAPCfCount;
    }

    public int getFRCAPCmCount() {
        return FRCAPCmCount;
    }

    public double getTumorChemoAvg() {
        return tumorChemoAvg;
    }

    public double getCCL1921Avg() {
        return CCL1921Avg;
    }

    public double getCXCL13Avg() {
        return CXCL13Avg;
    }

    public boolean getTumorDead() {
        return tumorDead;
    }

    public boolean getFRCAPCf() {
        return FRCAPCf;
    }

    public boolean getFRCAPCm() {
        return FRCAPCm;
    }

    public String getTumorKillTime() {
        if (print) {
            return days + " Days " + hours + " Hours " + minutes + " Minutes ";
        } else {
            return null;
        }
    }
}