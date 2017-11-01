package Models.SusanCode;

import Framework.Tools.FileIO;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

import Framework.Gui.GuiLabel;

/**
 * Created by susanlzhou on 7/10/17.
 */

class ReturnVals {
    //this object will hold the results of each run
    int[][] cellCounts;
    double[][] chemokineAvgs;
    String[] parameters;
    String[] trial;

    ReturnVals(int time) {
        cellCounts = new int[12][time];
        chemokineAvgs = new double[3][time];
        parameters = new String[9];
        trial = new String[24];
    }
}

public class Simulations {
    static int timeStep = 1;
    //int time = 50/timeStep;
    //int time = 21600/timeStep; // 15 days
    static int time = 2880 / timeStep; // 16 days
    static int nRuns = 20;
    //    static String condition = "FRC = 10 higher chemokine threshold";
    static String condition = "chemDegFRC = 0";

    static public void WriteCellCounts(ArrayList<ReturnVals> allRes, FileIO out, String title, int countIndex, String condition) {
        out.Write(title + "\n");
        double[] avg = new double[time];
        for (ReturnVals res : allRes) {
            out.WriteDelimit(res.cellCounts[countIndex], ",");
            out.Write("\n");
            for (int i = 0; i < time; i++) {
                avg[i] += res.cellCounts[countIndex][i];
            }
        }
        out.Write(title + " avg - " + condition + ",");
        for (int i = 0; i < time; i++) {
            avg[i] = (int) (avg[i] / nRuns * 1000);
            avg[i] = avg[i] / 1000;
        }
        out.WriteDelimit(avg, ",");
        out.Write("\n");
    }

    static public void WriteChemokine(ArrayList<ReturnVals> allRes, FileIO out, String title, int countIndex, String condition) {
        out.Write(title + "\n");
        double[] avg = new double[time];
        for (ReturnVals res : allRes) {
            out.WriteDelimit(res.chemokineAvgs[countIndex], ",");
            out.Write("\n");
            for (int i = 0; i < time; i++) {
                avg[i] += res.chemokineAvgs[countIndex][i];
            }
        }
        out.Write(title + " avg - " + condition + ",");
        for (int i = 0; i < time; i++) {
            avg[i] = (int) (avg[i] / nRuns * 1000);
            avg[i] = avg[i] / 1000;
        }
        out.WriteDelimit(avg, ",");
        out.Write("\n");
    }

    static public void WriteParameters(ArrayList<ReturnVals> allRes, FileIO out, String title) {
        out.Write(title);
        for (ReturnVals res : allRes) {
            out.Write("\n");

            out.WriteDelimit(res.parameters, ",");

        }
        out.Write("\n");
    }

    //static int count = 0;
    public static void main(String[] args) {
//        FileIO out=new FileIO("TLSResults" + args[0] + ".csv","w");
        FileIO out = new FileIO("TLSResults.csv", "w");
//        FileIO paramSweep = new FileIO("ParamSweepResults" + args[0] + ".csv", "w");
        FileIO paramSweep = new FileIO("ParamSweepResults.csv", "w");

        ArrayList<ReturnVals>runOutputs=new ArrayList<>(nRuns);
        for (int i = 0; i < nRuns; i++) {
            runOutputs.add(null);
        }
        MultiThread(nRuns, 8, (iThread) -> {
            Random rn = new Random();
            int numFRC = 100; // numFRC more concentrated towards smaller values
            //int numFRC = 0; // numFRC more concentrated towards smaller values
            //int patchSize = (int) (Math.random() * 5 + 9);
            int patchSize = 13;

            //int numNewCells = rn.nextInt(2) + 2;
            int numNewCells = 1;
            double chemProd = 0.1;
            double chemConsume = 0.2;
            double chemDegTumor = .005;
            double chemDegFRC = 0.04; // -- here
            double chemThreshold = 1;
            int numOfPatches = 1;
//            double chemProd = rn.nextDouble()/2;
//            double chemConsume = rn.nextDouble();
//            double chemDegTumor = (rn.nextDouble()* rn.nextDouble()* rn.nextDouble())/2;
//            double chemDegFRC =  (rn.nextDouble()* rn.nextDouble()* rn.nextDouble())/2;
//            double chemThreshold = rn.nextDouble();
            //the function that will run in parallel is specified here
            System.out.println("Started Thread: " + iThread);
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
            GuiLabel TimerLabel = new GuiLabel("Time: ");

            //FileIO outputFileTLSResults = new FileIO("temp.csv", "w");

            ModelGrid model = new ModelGrid(120, 120, numFRC, numNewCells, chemProd, chemConsume, chemDegTumor, chemDegFRC, chemThreshold, patchSize, numOfPatches, InactiveTCellLbl, PInactiveTCellLbl, ActiveTCellLbl, InactiveBCellLbl, ActiveBCellLbl, InactiveDCellLbl, DendFCellLbl, DendMCellLbl, TumorCellLbl, FRCiCellLbl, FRCAPCfCellLbl, FRCAPCmCellLbl, TimerLabel);
            ReturnVals ret = new ReturnVals(time);
            ret.parameters[0] = "iThread = " + iThread;
            ret.parameters[1] = "numFRC = " + numFRC;
            ret.parameters[2] = "numNewCells = " + numNewCells;
            ret.parameters[3] = "chemProd = " + chemProd;
            ret.parameters[4] = "chemConsume = " + chemConsume;
            ret.parameters[5] = "chemDegTumor = " + chemDegTumor;
            ret.parameters[6] = "chemDegFRC = " + chemDegFRC;
            ret.parameters[7] = "chemThreshold = " + chemThreshold;
            ret.parameters[8] = "patchSize = " + patchSize;
            ret.trial[0] = numFRC + "";
            ret.trial[1] = numNewCells + "";
            ret.trial[2] = chemProd + "";
            ret.trial[3] = chemConsume + "";
            ret.trial[4] = chemDegTumor + "";
            ret.trial[5] = chemDegFRC + "";
            ret.trial[6] = chemThreshold + "";
            ret.trial[7] = patchSize + "";
            boolean tumorFirst = false;
            boolean FRCAPCfFirst = false;
            boolean FRCAPCmFirst = false;
            for (int i = 0; i < time; i++) {
                model.Step();

                if (model.getTumorDead() && !tumorFirst) {
                    tumorFirst = true;
                    ret.trial[8] = i + "";
                }

                if (model.getFRCAPCf() && !FRCAPCfFirst) {
                    FRCAPCfFirst = true;
                    ret.trial[19] = i + "";
                }

                if (model.getFRCAPCm() && !FRCAPCmFirst) {
                    FRCAPCmFirst = true;
                    ret.trial[20] = i + "";
                }
//                String [] argument = new String [1];
//                argument[0] = iThread + "";
//                ModelGrid.main(argument);
                ret.cellCounts[0][i] = model.getTumorCellCount();
                ret.cellCounts[1][i] = model.gettCELLICount();
                ret.cellCounts[2][i] = model.gettCELLPICount();
                ret.cellCounts[3][i] = model.gettCELLACount();
                ret.cellCounts[4][i] = model.getbCELLICount();
                ret.cellCounts[5][i] = model.getbCELLACount();
                ret.cellCounts[6][i] = model.getdCELLICount();
                ret.cellCounts[7][i] = model.getdCellFCount();
                ret.cellCounts[8][i] = model.getdCellMCount();
                ret.cellCounts[9][i] = model.getFRCiCount();
                ret.cellCounts[10][i] = model.getFRCAPCfCount();
                ret.cellCounts[11][i] = model.getFRCAPCmCount();
                ret.chemokineAvgs[0][i] = model.getTumorChemoAvg();
                ret.chemokineAvgs[1][i] = model.getCCL1921Avg();
                ret.chemokineAvgs[2][i] = model.getCXCL13Avg();
            }
            //ret.trial[8] = model.getTumorDeathTime() + "";
            ret.trial[9] = model.getTumorCellCount() + "";
            ret.trial[10] = model.gettCELLICount() + "";
            ret.trial[11] = model.gettCELLPICount() + "";
            ret.trial[12] = model.gettCELLACount() + "";
            ret.trial[13] = model.getbCELLICount() + "";
            ret.trial[14] = model.getbCELLACount() + "";
            ret.trial[15] = model.getdCELLICount() + "";
            ret.trial[16] = model.getdCellFCount() + "";
            ret.trial[17] = model.getdCellMCount() + "";
            ret.trial[18] = model.getFRCiCount() + "";
            //ret.trial[19] = model.getFRCAPCfDeathTime() + "";
            //ret.trial[20] = model.getFRCAPCmDeathTime() + "";
            ret.trial[21] = model.getTumorChemoAvg() + "";
            ret.trial[22] = model.getCCL1921Avg() + "";
            ret.trial[23] = model.getCXCL13Avg() + "";
//            if (ret.trial[8] == null)
//            {
//                ret.trial[8] = time + model.getTumorCellCount() + "";
//            }
            for (int k = 0; k < ret.trial.length; k++) {
                if (ret.trial[k] == null) {
                    ret.trial[k] = "0";
                }
            }
            System.out.println("Finished thread: " + iThread);
            //count++;
            runOutputs.set(iThread,ret);
        });
        //after all runs finish, we loop through the array of ReturnVals objects and write out their data
        //FileIO out=new FileIO("TLSResults.csv","w");
        WriteParameters(runOutputs, out, "Parameters");
        WriteCellCounts(runOutputs, out, "TumorCellCount", 0, condition);
        WriteCellCounts(runOutputs, out, "TCellI", 1, condition);
        WriteCellCounts(runOutputs, out, "TCellPI", 2, condition);
        WriteCellCounts(runOutputs, out, "TCellA", 3, condition);
        WriteCellCounts(runOutputs, out, "BCellI", 4, condition);
        WriteCellCounts(runOutputs, out, "BCellA", 5, condition);
        WriteCellCounts(runOutputs, out, "DCellI", 6, condition);
        WriteCellCounts(runOutputs, out, "DCellF", 7, condition);
        WriteCellCounts(runOutputs, out, "DCellM", 8, condition);
        WriteCellCounts(runOutputs, out, "FRCI", 9, condition);
        WriteCellCounts(runOutputs, out, "FRCAPCf", 10, condition);
        WriteCellCounts(runOutputs, out, "FRCAPCm", 11, condition);
        WriteChemokine(runOutputs, out, "TumorChemoAvg", 0, condition);
        WriteChemokine(runOutputs, out, "CCL19/21", 1, condition);
        WriteChemokine(runOutputs, out, "CXCL13", 2, condition);

        out.Close();
        paramSweep.Write("numFRC, numNewCells, chemProd, chemConsume, chemDegTumor, chemDegFRC, chemThreshold, patchSize, TumorDeathTime, FinalTumorCellCount, FinalTCellICount, FinalTCellPICount, FinalTCellACount, FinalBCellICount, FinalBCellACount, FinalDCellICount, FinalDCellFCount, FinalDCellMCount, FinalFRCICount, FRCAPCfActTime, FRCAPCmActTime, TumorChemoAvg, CCL19/21, CXCL13");
        paramSweep.Write("\n");
        for (ReturnVals ret : runOutputs) {
            paramSweep.WriteDelimit(ret.trial, ",");
            paramSweep.Write("\n");
        }
        paramSweep.Close();
    }

}