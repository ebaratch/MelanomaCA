package Models.ManualModels.ExtensionExample;

import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Utils;

import java.util.Random;

/**
 * Created by rafael on 9/3/17.
 */

class HybridCell<A extends HybridCell,G extends HybridModel<A>> extends CACell<A,G>{

    public void CellReaction(){
        G().drug.Mul(Isq(),G().DRUG_UPTAKE);
    }

    @Override
    public double GetDeathProb(){
        if(isResistant){
            return G().DEATH_RATE_RESISTANT;
        }
        return G().DEATH_RATE_SENSITIVE+(G().drug.Get(Isq())*G().DEATH_PROB_CONC_SCALE);
    }
}

public class HybridModel<A extends HybridCell> extends CAModel<A> {
    public PDEGrid2D drug;

    public double DRUG_START=1000;
    public double DRUG_PERIOD=2000;
    public double DRUG_DURATION=250;
    public double DRUG_DIFF_RATE=0.25;
    public double DRUG_UPTAKE=0.99;
    public double DEATH_PROB_CONC_SCALE=0.04;
    public double DRUG_BOUNDARY_ON=1;
    public double DRUG_BOUNDARY_OFF=0;

    public HybridModel(int xDim, int yDim, Class<A> agentClass, Random rn) {
        super(xDim, yDim, agentClass, rn);
        drug=new PDEGrid2D(xDim,yDim);
    }

    public static void main(String[] args) {
        HybridModel m =new HybridModel(100,100,HybridCell.class,new Random());
        m.CreateTumor(4,0.5);
        m.RunWithGui("Hybrid StaticModel",100000,5,2);
    }

    @Override
    public void GridStep(){
        ReactionDiffusionStep();
        StepCells();
    }

    public void ReactionDiffusionStep(){
        for (HybridCell c : this) {
            c.CellReaction();
        }
        if(GetTick()>DRUG_START&&(GetTick()-DRUG_START)%DRUG_PERIOD<DRUG_DURATION) {
            drug.Diffusion(DRUG_DIFF_RATE, DRUG_BOUNDARY_ON);
        }
        else{
            drug.Diffusion(DRUG_DIFF_RATE);
        }
    }

    public void DrawModel(GuiGridVis vis){
        for (int i = 0; i < vis.length; i++) {
            HybridCell drawMe = GetAgent(i);
            if (drawMe == null) {
                vis.SetPix(i, Utils.HeatMapRGB(drug.Get(i)));
            } else if (drawMe.isResistant) {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 1, (double) 0));
            } else {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 1));
            }
        }
    }

}
