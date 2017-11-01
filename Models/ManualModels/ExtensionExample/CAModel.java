package Models.ManualModels.ExtensionExample;

import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by rafael on 9/3/17.
 */
class CACell<A extends CACell,G extends CAModel<A>> extends StaticCell<A,G>{
    public void CellStep(){
        if(G().rn.nextDouble()<GetDeathProb()){
            Die();
        }
        else if(G().rn.nextDouble()<GetDivProb()){
            Divide();
        }
    }
    public double GetDivProb(){
        if(isResistant){
            return G().DIV_RATE_RESISTANT;
        }
        return G().DIV_RATE_SENSITIVE;
    }
    public double GetDeathProb(){
        if(isResistant){
            return G().DEATH_RATE_RESISTANT;
        }
        return G().DEATH_RATE_SENSITIVE;
    }
    public void Divide(){
        int nEmptySpaces=G().HoodToEmptyIs(G().divHood,G().divIs,Xsq(),Ysq());
        if(nEmptySpaces>0) {
            CACell daughter = G().NewAgentSQ(G().divIs[G().rn.nextInt(nEmptySpaces)]);
            daughter.CellSetup(this.isResistant);
        }
    }
    public void Die(){
       Dispose();
    }
}

public class CAModel<A extends CACell> extends StaticModel<A> {
    int[] divHood = MooreHood(false);
    int[] divIs = new int[divHood.length];

    public double DIV_RATE_SENSITIVE = 0.0025;
    public double DIV_RATE_RESISTANT = 0.001;
    public double DEATH_RATE_SENSITIVE = 0.0001;
    public double DEATH_RATE_RESISTANT = 0.0001;

    public CAModel(int xDim, int yDim, Class<A> agentClass, Random rn) {
        super(xDim, yDim, agentClass, rn);
    }

    public static void main(String[] args) {
        CAModel m = new CAModel(100, 100, CACell.class, new Random());
        m.CreateTumor(4, 0.5);
        m.RunWithGui("CA StaticModel Example", 100000, 5, 2);
    }

    @Override
    public void GridStep() {
        StepCells();
    }

    public void StepCells(){
        for (CACell c : this) {
            c.CellStep();
        }
        CleanShuffInc(rn);
    }
}
