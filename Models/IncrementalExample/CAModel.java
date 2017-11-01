package Models.IncrementalExample;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.Random;

import static Framework.Utils.MooreHood;


class CACell extends AgentSQ2Dunstackable<CAModel> {
    boolean isResistant;

    public void CellSetup(boolean isResistant){
        this.isResistant=isResistant;
    }

    //NEW CODE
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
    //NEW CODE
}

public class CAModel extends AgentGrid2D<CACell> {
    final Random rn;

    public CAModel(int xDim, int yDim, Random rn) {
        super(xDim, yDim, CACell.class);
        this.rn=rn;
    }

    public static void main(String[] args) {
        CAModel m=new CAModel(100,100,new Random());
        m.CreateTumor(4,0.5);
        m.RunWithGui("Static StaticModel",10000,5,2);
    }

    public void CreateTumor(int radius,double resistantProb){
        int[]cellCoords= Utils.CircleHood(true,radius);
        int[]cellIs=new int[cellCoords.length/2];
        int cellsToPlace= HoodToEmptyIs(cellCoords,cellIs,xDim/2,yDim/2);

        for (int i = 0; i < cellsToPlace; i++) {
            CACell c=NewAgentSQ(cellIs[i]);
            c.CellSetup(rn.nextDouble()<resistantProb);
        }
    }

    public void RunWithGui(String guiTitle,int timesteps,int visScale,int tickRateMillis){
        GuiWindow win=new GuiWindow(guiTitle,true);
        GuiGridVis vis=new GuiGridVis(xDim,yDim,visScale);
        win.AddCol(0, vis);
        win.RunGui();
        TickTimer timer=new TickTimer();

        for (int i = 0; i < timesteps; i++) {
            timer.TickPause(tickRateMillis);
            GridStep();
            DrawModel(vis);
        }
        win.Dispose();
    }

    public void GridStep(){
        StepCells();
    }
    public void StepCells(){
        for (CACell c : this) {
            c.CellStep();
        }
        CleanShuffInc(rn);
    }

    public void DrawModel(GuiGridVis vis){
        for (int i = 0; i < vis.length; i++) {
            CACell drawMe = GetAgent(i);
            if (drawMe == null) {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 0));
            } else if (drawMe.isResistant) {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 1));
            } else {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 1, (double) 0));
            }
        }
    }

    //NEW CODE
    int[] divHood = MooreHood(false);
    int[] divIs = new int[divHood.length];
    public double DIV_RATE_SENSITIVE = 0.0025;
    public double DIV_RATE_RESISTANT = 0.001;
    public double DEATH_RATE_SENSITIVE = 0.0001;
    public double DEATH_RATE_RESISTANT = 0.0001;
    //NEW CODE

}
