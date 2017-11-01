package Models.Raf.QuackModels.QuackMarkModelExact;

import Framework.Extensions.ClinicianSim;
import Framework.Extensions.MarkModel_II.MarkModelDrugs.*;
import Framework.Gui.GuiGridVis;
import Framework.Interfaces.TreatableTumor;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Rafael on 10/25/2017.
 */
public class MarkModelQuack extends MarkModelPlusDrugs implements TreatableTumor{
    ArrayList<Drug> playerDrugs;
    public MarkModelQuack(int x, int y, boolean reflectiveBoundary, boolean setupConstants, Random rn) {
        super(x, y, reflectiveBoundary, setupConstants, rn);
        playerDrugs=new ArrayList<>();
    }

    @Override
    public void AddDrug(Drug addMe){
        super.AddDrug(addMe);
        if(addMe.controllable) {
            this.playerDrugs.add(addMe);
        }
    }
    @Override
    public void Draw(GuiGridVis vis, GuiGridVis alphaVis,boolean[] switchVals) {
        DrawCells(vis);
    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {
        this.intensities=treatmentVals;
        StepAll();
    }

    @Override
    public String[] GetTreatmentNames() {
        String[]names=new String[playerDrugs.size()];
        for (int i = 0; i < names.length; i++) {
            names[i]=playerDrugs.get(i).name;
        }
        return names;
    }

    @Override
    public int[] GetTreatmentColors() {
        int[]colors=new int[playerDrugs.size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i]=playerDrugs.get(i).color;
        }
        return colors;
    }

    @Override
    public int GetNumIntensities() {
        return 3;
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
        double sum=0;
        for (Drug d : this.GetAllModules()) {
            sum+=d.GetTox();
        }
        return sum;
    }

    @Override
    public double GetBurden() {
        return this.GetTypePop(TUMOR)*1.0/this.length;
    }

    @Override
    public double GetMaxTox() {
        return this.GetAllModules().size()/2.0;
    }

    @Override
    public double GetMaxBurden() {
        return 0.5;
    }

//    @Override
//    public String[] GetSwitchNames() {
//        String[]ret=new String[playerDrugs.size()];
//        for (int i = 0; i < playerDrugs.size(); i++) {
//            ret[i]=playerDrugs.get(i).name;
//        }
//        return ret;
//    }
//
//    @Override
//    public boolean AllowMultiswitch() {
//        return false;
//    }

    @Override
    public void SetupConstructors() {
        this._SetupAgentListConstructor(DrugCell.class);
    }

    public static void main(String[] args) {
        MarkModelQuack model=new MarkModelQuack(20,20,false,false,new Random());
        model.AddDrug(new Chemo(model));
        model.SetupConstants();
        model.InitAll(0.8,5);
        ClinicianSim sim=new ClinicianSim(model,100,10,25,5,25,30,100,1000);
        sim.RunGui();
        sim.RunModel();
    }
}
