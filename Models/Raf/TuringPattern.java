package Models;

import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Utils;

import java.util.Random;

/**
 * Created by Rafael on 10/27/2017.
 */
public class TuringPattern {
    PDEGrid2D grid1;
    PDEGrid2D grid2;
    GuiGridVis vis1;
    GuiGridVis vis2;
    GuiWindow win;
    Random rn=new Random();
    double DIFF_CONST=0.1;
    double A=0.1;
    double B=1;
    double C=619.45;//TODO: fill in values for C and D
    double D=70.85;
    double K=0.5;
    void ReactionDiffusion(){
        grid1.Diffusion(DIFF_CONST);
        for (int i = 0; i < grid1.length; i++) {
            double curr=grid1.Get(i);
            double currSq=curr*curr;
            grid1.Add(i,C*(A-B*curr+(currSq/(grid2.Get(i)*(1+K*currSq)))));
        }
        grid2.Diffusion(DIFF_CONST);
        for (int i = 0; i < grid2.length; i++) {
            double curr1=grid1.Get(i);
            grid1.Add(i,C*(curr1*curr1-grid2.Get(i)));
        }
    }
    void Init(){
        grid1.SetAll(0.84);
        grid2.SetAll(0.7);
        Perturb(grid1,0.1);
    }
    void Perturb(PDEGrid2D grid,double delta){
        grid.Add(rn.nextInt(grid.length),rn.nextDouble()*delta*2-delta);

    }
    void Draw(PDEGrid2D grid, GuiGridVis vis){
        for (int i = 0; i < grid.length; i++) {
            vis.SetPix(i, Utils.HeatMapBGR(grid.Get(i)));
        }
    }

    void Run(int pauseMS){
        while(true){
            win.TickPause(pauseMS);
            ReactionDiffusion();
            Draw(grid1,vis1);
            Draw(grid2,vis2);
        }
    }
    TuringPattern(int x,int y,int visScale){
        //set up diffusibles
        grid1=new PDEGrid2D(x,y);
        grid2=new PDEGrid2D(x,y);
        //set up visualization
        vis1=new GuiGridVis(x,y,visScale);
        vis2=new GuiGridVis(x,y,visScale);
        win=new GuiWindow("Gierer Meinhardt",true);
        win.AddCol(0,vis1);
        win.AddCol(1,vis2);
        win.RunGui();
    }

    public static void main(String[] args) {
        int x=100,y=100,scale=5,pause=0;
        TuringPattern tp=new TuringPattern(x,y,scale);
        tp.Init();
        tp.Run(0);
    }
}
