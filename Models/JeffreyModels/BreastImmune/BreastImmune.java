package Models.JeffreyModels.BreastImmune;

import Framework.Extensions.ClinicianSim;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Utils;
import Framework.Interfaces.TreatableTumor;

import java.io.Serializable;
import java.util.Random;

import static Framework.Utils.*;

//an example of the ClinicianSim in action
public class BreastImmune extends AgentGrid2D<Cell> implements TreatableTumor,Serializable{
    Random rn=new Random();

    //model constants
    public double divProb = 0.005, deathProb = 0.001, immuneResProb = 0.1, aromataseResProb = 0.1;
    public double immuneResMutRate = 0.001, estrogenResMutRate = 0.001;


    // aromatase inhibitor params
    public double aromataseDiffRate = 0.25, aromataseUptakeRate=0.9, aromataseDeathFactor = 0.7;

    // immune presence params
    public double immuneDiffRate = 0.05, immuneDeathFactor = 0.4, immuneDecayRate = 0.998,immuneKillStim = 0.01;

    //internal model objects
    public PDEGrid2D immunePresence;
    public PDEGrid2D aromataseInhibitor;
    public int[] divHood = MooreHood(false);
    public int[] divIs = new int[divHood.length / 2];

    public BreastImmune(int xDim, int yDim) {
        super(xDim, yDim, Cell.class);
        immunePresence = new PDEGrid2D(xDim, yDim);
        aromataseInhibitor = new PDEGrid2D(xDim,yDim);
    }
    public void InitTumor(int radius,Random rn) {
        //get a list of indices that fill a circle at the center of the grid
        int[] circleCoords = CircleHood(true, radius);
        int[] cellIs = new int[circleCoords.length / 2];
        int cellsToPlace = HoodToEmptyIs(circleCoords, cellIs, xDim / 2, yDim / 2);
        //place a new tumor cell at each index
        for (int i = 0; i < cellsToPlace; i++) {
            Cell seedCell = NewAgentSQ(cellIs[i]);

            if (rn.nextDouble() < immuneResProb) {
                seedCell.immuneResistant = true;
            }
            if (!seedCell.immuneResistant) {
                if (rn.nextDouble() < aromataseResProb) {
                    seedCell.estrogenResistant = true;
                }
            }

        }
    }

    @Override
    public void Draw(GuiGridVis vis, GuiGridVis alphaVis, boolean[] switchVals) {
        for (int i = 0; i < vis.length; i++) {
            Cell drawMe = GetAgent(i);
            if (drawMe == null) {
                vis.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
                //vis.SetColorHeat(i, (int)(drug.GetPix(i)*20)/20.0); //drug conc (heat colormap)
            } else if (drawMe.estrogenResistant && drawMe.immuneResistant){
                vis.SetPix(i, RGB((double) 1, (double) 0, (double) 0));
            } else if (!drawMe.estrogenResistant && drawMe.immuneResistant) {
                vis.SetPix(i, RGB((double) 1, (double) 1, (double) 1));
            } else if (drawMe.estrogenResistant && !drawMe.immuneResistant) {
                vis.SetPix(i, RGB((double) 0, (double) 1, (double) 0));
            } else {
                vis.SetPix(i, RGB((double) 0, (double) 0, (double) 1));
            }
            //alphaVis.SetColorHeatAlpha(i, immunePresence.GetPix(i)*8.0/3.0, 1);
            alphaVis.SetPix(i, SetAlpha(HeatMapRGB(immunePresence.Get(i)*80.0/3.0), Utils.Bound(immunePresence.Get(i)*80.0,0,0.5)));
            //alphaVis.SetColorHeatAlpha(i, aromataseInhibitor.GetPix(i)*8.0/3.0, Utils.Bound(aromataseInhibitor.GetPix(i)*8.0,0,0.5)); //drug conc (heat colormap)

        }

    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {



        for (int i = 0; i < 10; i++) {

            for (Cell cell : this) {
                cell.CellStep(rn);
            }
            aromataseInhibitor.Diffusion(aromataseDiffRate,treatmentVals[0]);
            immunePresence.Diffusion(immuneDiffRate);
            immunePresence.MulAll(immuneDecayRate);
            CleanShuffInc(rn);
        }
    }


    @Override
    public String[] GetTreatmentNames() {
        return new String[]{"Aromatase Inhibitor"};
    }

    @Override
    public int[] GetTreatmentColors() {
        return new int[]{HSBColor(1.0/6,1,1)};
    }

    @Override
    public int GetNumIntensities() {
        return 5;
    }

    @Override
    public int VisPixX() {
        return xDim;
    }

    @Override
    public int VisPixY() {
        return yDim;
    }

    @Override
    public double GetTox() {
        return aromataseInhibitor.GetAvg()*1.5;
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
        BreastImmune model=new BreastImmune(100,100);
        model.InitTumor(15,new Random(1));
        ClinicianSim ccl=new ClinicianSim(model,600,10,5,1,25,25,10,100);
        ccl.RunGui();
        ccl.RunModel();
    }

    public void SetupConstructors(){
        _SetupAgentListConstructor(Cell.class);
    }
}

class Cell extends AgentSQ2Dunstackable<BreastImmune> {

    // immune resistant (via) PD-L1 and estrogen resistant are orthogonal axes


    public boolean immuneResistant;
    public boolean estrogenResistant;

    public void CellStep(Random rn) {

        G().aromataseInhibitor.Mul(Isq(), G().aromataseUptakeRate);

        //Chance of Death, depends on resistance and drug concentration
        if (rn.nextDouble() < G().deathProb + G().immunePresence.Get(Isq()) * G().immuneDeathFactor + (estrogenResistant ? 0 : G().aromataseInhibitor.Get(Isq()) * G().aromataseDeathFactor)) {
            G().immunePresence.Add(Isq(),G().immuneKillStim);
            Dispose();
        }

        if (rn.nextDouble() < G().divProb) {
            int nEmptySpaces = G().HoodToEmptyIs(G().divHood, G().divIs, Xsq(), Ysq());
            //If any empty spaces exist, randomly choose one and create a daughter cell there
            if (nEmptySpaces > 0) {
                Cell daughter = G().NewAgentSQ(G().divIs[rn.nextInt(nEmptySpaces)]);
                daughter.immuneResistant = this.immuneResistant;
                daughter.estrogenResistant = this.estrogenResistant;

                if (rn.nextDouble() < G().immuneResMutRate) {
                    daughter.immuneResistant = true;
                }
                if (rn.nextDouble() < G().estrogenResMutRate) {
                    daughter.estrogenResistant = true;
                }

            }
        }
    }
}

