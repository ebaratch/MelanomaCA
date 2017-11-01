package Models.IncrementalExample;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.Random;


class StaticCell extends AgentSQ2Dunstackable<StaticModel> {
    boolean isResistant;

    public void CellSetup(boolean isResistant){
        this.isResistant=isResistant;
    }
}

public class StaticModel extends AgentGrid2D<StaticCell> {
    final Random rn;

    public StaticModel(int xDim, int yDim, Random rn) {
        super(xDim, yDim, StaticCell.class);
        this.rn=rn;
    }

    public static void main(String[] args) {
        StaticModel m=new StaticModel(100,100,new Random());
        m.CreateTumor(4,0.5);
        m.RunWithGui("Static StaticModel",10000,5,2);
    }

    public void CreateTumor(int radius,double resistantProb){
        int[]cellCoords= Utils.CircleHood(true,radius);
        int[]cellIs=new int[cellCoords.length/2];
        int cellsToPlace= HoodToEmptyIs(cellCoords,cellIs,xDim/2,yDim/2);

        for (int i = 0; i < cellsToPlace; i++) {
            StaticCell c=NewAgentSQ(cellIs[i]);
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
    }

    public void DrawModel(GuiGridVis vis){
        for (int i = 0; i < vis.length; i++) {
            StaticCell drawMe = GetAgent(i);
            if (drawMe == null) {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 0));
            } else if (drawMe.isResistant) {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 0, (double) 1));
            } else {
                vis.SetPix(i,Utils.RGB((double) 0, (double) 1, (double) 0));
            }
        }
    }
}
