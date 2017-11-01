package Models.JeffreyModels.NonCrossResistance;


import Framework.Extensions.ClinicianSim;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Interfaces.TreatableTumor;

import java.io.Serializable;
import java.util.Random;

import static Framework.Utils.*;

public class NonCrossRes extends AgentGrid2D<Cell> implements TreatableTumor,Serializable{
    //model constants
    public double divProb = 0.025, divProbRes = 0.01, deathProb = 0.001, drugDiffRate = 0.25, drugUptake = 0.99, drugDeath = 0.3, mutRate1 = 0.0001, mutRate2 = 0.005;

    //internal model objects
    public PDEGrid2D drug1;
    public PDEGrid2D drug2;
    public int[] divHood = MooreHood(false);
    public int[] divIs = new int[divHood.length / 2];
    Random rn=new Random();

    public NonCrossRes(int xDim, int yDim) {
        super(xDim, yDim, Cell.class);
        drug1 = new PDEGrid2D(xDim, yDim); // targets isRes2
        drug2 = new PDEGrid2D(xDim, yDim); // targets isRes1

    }
    public void InitTumor(int radius,double resistantProb1, double resistantProb2, Random rn) {
        //get a list of indices that fill a circle at the center of the grid
        int[] circleCoords = CircleHood(true, radius);
        int[] cellIs = new int[circleCoords.length / 2];
        int cellsToPlace = HoodToEmptyIs(circleCoords, cellIs, xDim / 2, yDim / 2);
        //place a new tumor cell at each index
        for (int i = 0; i < cellsToPlace; i++) {
            Cell seedCell = NewAgentSQ(cellIs[i]);
            seedCell.isRes2 = rn.nextDouble() < resistantProb1; // none start as Res1
            if (!seedCell.isRes2) {
                seedCell.isRes1 = rn.nextDouble() < resistantProb2; // none start as Res1
            }
        }
    }

    @Override
    public void Draw(GuiGridVis vis, GuiGridVis alphaVis, boolean[] switchVals) {
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                Cell drawMe = GetAgent(x,y);
                if (drawMe == null) {
                    vis.SetPix(x, y, RGB((double) 0, (double) 0, (double) 0));
                } else if (drawMe.isRes1 && drawMe.isRes2) {
                    //vis.SetBlock(x, y, 1, 0, 0); // DOUBLY RESISTANT
                    vis.SetPix(x, y,HSBColor((float)0.0/6, (float) 1, (float) 1));
                } else if (drawMe.isRes1 && !drawMe.isRes2){
                    //vis.SetBlock(x, y, 0, 1, 0); //RESISTANT to drug1
                    vis.SetPix(x, y,HSBColor((float)2.0/6, (float) 1, (float) 1));
                } else if (drawMe.isRes2 && !drawMe.isRes1){
                    //vis.SetBlock(x, y, 1, 1, 0); //RESISTANT to drug2 (yellow)
                    vis.SetPix(x, y,HSBColor((float)1.0/6, (float) 1, (float) 1));
                } else {
                    vis.SetPix(x, y, RGB((double) 0, (double) 0, (double) 1));
                    vis.SetPix(x, y,HSBColor((float)3.0/6, (float) 1, (float) 1));
                }
                vis.SetPix(x+xDim,y,HeatMapGBR(drug1.Get(x,y)));
                vis.SetPix(x+2*xDim, y, HeatMapRGB(drug2.Get(x,y)));
            }
        }

    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {
        for (int i = 0; i < 10; i++) {

            for (Cell cell : this) {
                cell.CellStep(rn);
            }
            //check if drug should enter through the boundaries
            drug1.Diffusion(drugDiffRate, treatmentVals[0]);
            drug2.Diffusion(drugDiffRate, treatmentVals[1]);
            CleanShuffInc(rn);
        }
    }


    @Override
    public String[] GetTreatmentNames() {
        return new String[]{"Drug Yellow","Drug Green"};
    }

    @Override
    public int[] GetTreatmentColors() {
        return new int[]{HSBColor(1.0/6,1,1),HSBColor(2.0/6,1,1)};
    }

    @Override
    public int GetNumIntensities() {
        return 4;
    }

    @Override
    public int VisPixX() {
        return xDim*3;
    }

    @Override
    public int VisPixY() {
        return yDim;
    }

    @Override
    public double GetTox() {
        return (drug1.GetAvg() + drug2.GetAvg());
    }

    @Override
    public double GetBurden() {
        return GetPop()*1.0/length;
    }

    @Override
    public double GetMaxTox() {
        return 0.5;
    }

    @Override
    public double GetMaxBurden() {
        return 0.5;
    }

//    @Override
//    public String[] GetSwitchNames() {
//        return new String[0];
//    }
//
//    @Override
//    public boolean AllowMultiswitch() {
//        return false;
//    }


    public static void main(String[] args) {
        NonCrossRes model=new NonCrossRes(100,100);
        //model.SetupTumor(10,0.8, 0.01,new Random(1));
        model.InitTumor(10,0.4, 0.2,new Random(1));
        ClinicianSim ccl=new ClinicianSim(model,500,10,5,2,26,25,10,100);
        ccl.RunGui();
        ccl.RunModel();
    }
    public void SetupConstructors(){
        _SetupAgentListConstructor(Cell.class);
    }
}

class Cell extends AgentSQ2Dunstackable<NonCrossRes> {
    public boolean isRes1;
    public boolean isRes2;

    public void CellStep(Random rn) {
        //Consumption of Drug
        G().drug1.Mul(Isq(), G().drugUptake);
        //Chance of Death, depends on resistance and drug concentration
        //if (rn.nextDouble() < G().DEATH_PROB + (isRes1 ? 0 : G().drug1.GetPix(Isq()) * G().DRUG_DEATH)) {
        if (rn.nextDouble() < G().deathProb + (isRes2 ? 0 : G().drug2.Get(Isq()) * G().drugDeath) + (isRes1 ? 0 : G().drug1.Get(Isq()) * G().drugDeath) ) {
            Dispose();
        }
        //Chance of Division, depends on resistance of either type
        else if (rn.nextDouble() < ((isRes1 || isRes2) ? G().divProbRes : G().divProb)) {
            int nEmptySpaces = G().HoodToEmptyIs(G().divHood, G().divIs, Xsq(), Ysq());
            //If any empty spaces exist, randomly choose one and create a daughter cell there
            if (nEmptySpaces > 0) {
                Cell daughter = G().NewAgentSQ(G().divIs[rn.nextInt(nEmptySpaces)]);
                daughter.isRes1 = this.isRes1;
                daughter.isRes2 = this.isRes2;

                if (!isRes1) {
                    if (rn.nextDouble() < G().mutRate1) {
                        daughter.isRes1 = true;
                    }
                }
                if (!isRes2) {
                    if (rn.nextDouble() < G().mutRate2) {
                        daughter.isRes2 = true;
                    }
                }
            }
        }
    }
}

