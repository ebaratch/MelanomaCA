package Models.Raf.Experimental.CANCERCRAFT;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;

import java.awt.*;
import java.util.Random;

import static Models.Raf.Experimental.CANCERCRAFT.Body.*;
import static Framework.Utils.*;

/**
 * Created by rafael on 9/14/17.
 */

class ImmuneSystem extends AgentGrid2D<ImmuneCell> {
    public ImmuneSystem(int x, int y) {
        super(x, y, ImmuneCell.class, false, false);
    }
    public void DrawCells(GuiGridVis vis){
        for (ImmuneCell c : this) {
            vis.SetPix(c.Bx(), c.By(), ImmuneCell.DrawCell(c));
        };
    }
}

class Stroma extends AgentGrid2D<BodyCell> {
    public Stroma(int x,int y){
        super(x,y,BodyCell.class,false,false);
    }
}

public class Body extends AgentGrid2D<BodyCell> {
    //uses moore hood, maybe hex later

    //so, what do fibroblasts do anyway? besides be all cool and stuff
    //synthesize all of ec matrix
    //transform to fibroblasts via emt
    //transform from emt to

    static final int SCALE =4;//scaling factor between cells and GridDiffs/Immune

    //names
    public static final String[] names={"ANTIBODY","SUGAR","ACID","CHEMOKINE","CELL","GENOME","IMMUNE","MAIN"};
    //chemicals
    public static final int ANTIBODY = 0, SUGAR = 1, ACID = 2, CHEMOKINE = 3, N_CHEMS = 4;
    //visualization numbers
    public static final int CELL = N_CHEMS, GENOME = N_CHEMS + 1, IMMUNE = N_CHEMS + 2, MAIN = N_CHEMS + 3, N_VISS = N_CHEMS + 4;
    public double[] initialConds = new double[]{0, 1, 0, 0};

    final ImmuneSystem immune;
    public Random rn;

    int[]moveHood=MooreHood(false);
    int[]moveIs=new int[moveHood.length/2];
    int[]moveOpts=new int[moveHood.length/2];
    final PDEGrid2D[] chems;

    public void Step() {
    }

    public void InitChems() {
        for (int i = 0; i < N_CHEMS; i++) {
            if (chems[i] == null) {
                chems[i] = new PDEGrid2D(xDim/ SCALE, yDim/ SCALE, false, false);
            }
            chems[i].SetAll(initialConds[i]);
        }
    }
    public void InitImmune(){

    }
    public void InitCells(){
    }
    public void Init(){
        InitChems();
        InitImmune();
    }

    public Body(int x, int y) {
        super(x* SCALE, y* SCALE, BodyCell.class, false, false);
        chems = new PDEGrid2D[N_CHEMS];
        immune=new ImmuneSystem(x,y);
    }


    public static void main(String[] args) {
        Body b = new Body(25, 25);
        b.Init();
        Window win=new Window(b,true);
        for (int i = 0; i < 1000; i++) {
            win.TickPause(100);
            win.Draw(b);
        }
    }
}
class Window {
    GuiWindow win;
    GuiGridVis[] viss;

    public Window(Body b, boolean active) {
        win = new GuiWindow("the main game", true, active);
        viss = new GuiGridVis[N_VISS];
        for (int i = 0; i < N_VISS; i++) {
            switch (i) {
                case MAIN:
                    viss[i] = new GuiGridVis(b.xDim, b.yDim, 4, 4, 4, active);
                    win.AddCol(0, new GuiLabel(names[i],4,1));
                    win.AddCol(0, viss[i]);
                    break;
                case CELL:
                    viss[i] = new GuiGridVis(b.xDim,b.yDim,1,1,1,active);
                    win.AddCol(i, new GuiLabel(names[i]));
                    win.AddCol(i, viss[i]);
                    break;
                case GENOME:
                    viss[i] = new GuiGridVis(b.xDim,b.yDim,1,1,1,active);
                    win.AddCol(i, new GuiLabel(names[i]));
                    win.AddCol(i, viss[i]);
                    break;
                case IMMUNE:
                    viss[i] = new GuiGridVis(b.xDim,b.yDim,1,1,1,active);
                    win.AddCol(i, new GuiLabel(names[i]));
                    win.AddCol(i, viss[i]);
                    break;
                default:
                    viss[i] = new GuiGridVis(b.xDim/ SCALE, b.yDim/ SCALE, SCALE, 1, 1, active);
                    win.AddCol(i,new GuiLabel(names[i]));
                    win.AddCol(i, viss[i]);
                    break;
            }
        }
        win.RunGui();
    }
    public void TickPause(int ms){
        win.TickPause(ms);
    }
    public void Draw(Body b){
        for (int i = 0; i < N_CHEMS; i++) {
            viss[i].DrawGridDiff(b.chems[i],(val)->{
                return HeatMapRBG(RescaleMinToMax(val, (double) 0, (double) 1));
            });
        }
        viss[CELL].DrawAgents(b,BodyCell::DrawPhenotype, Color.BLACK.getRGB());
        viss[GENOME].DrawAgents(b,BodyCell::DrawGenome,Color.BLACK.getRGB());
        b.immune.DrawCells(viss[IMMUNE]);
        viss[MAIN].DrawAgents(b,BodyCell::DrawPhenotype,Color.BLACK.getRGB());
        b.immune.DrawCells(viss[MAIN]);
    }
}
