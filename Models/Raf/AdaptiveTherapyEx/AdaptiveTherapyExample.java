package Models.Raf.AdaptiveTherapyEx;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

import java.util.Random;
import static Framework.Utils.*;

class Cell extends AgentSQ2Dunstackable<AdaptiveTherapyExample> {
    //internal cell type variable
    boolean isResistant;

    //sets cell type, called when cell is first born
    void Init(boolean isResistant){
        this.isResistant = isResistant;
    }

    //runs one cell step. cells can die, divide, or do neither
    void Step(){
        if(G().rn.nextDouble()<CalcDeathProb()){
            Dispose();
            return;
        }
        if(G().rn.nextDouble()<CalcDivProb()){
            Divide();
        }
    }

    //cell divides into an empty moore neighborhood position if one exists
    Cell Divide(){
        int nEmpty=G().HoodToEmptyIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        if(nEmpty==0){ return null; }
        Cell child=G().NewAgentSQ(G().divIs[G().rn.nextInt(nEmpty)]);
        child.Init(isResistant);
        return child;
    }

    //gets probability of division, based on resistance status
    double CalcDivProb(){
        if(isResistant){
            return G().DIV_PROB_RESIST;
        }
        else{
            return G().DIV_PROB_NORM;
        }
    }

    //gets probability of death, based on resistance status and drug concentration
    double CalcDeathProb(){
        if(isResistant){
            return G().DEATH_PROB_RESIST;
        }
        else{
            return G().DEATH_PROB_NORM+(G().drug.Get(Isq())*G().DEATH_PROB_CONC_SCALE);
        }
    }
}

class AdaptiveTherapyExample extends AgentGrid2D<Cell> {

    //run variables
    static int SIDE_LEN=100;
    static int TIMESTEPS=100000;
    static int TICK_PAUSE=0;
    static int VIS_SCALE=4;
    static int START_RAD=4;

    //model variables
    double DIV_PROB_NORM=0.0025;
    double DIV_PROB_RESIST=0.0010;
    double DEATH_PROB_RESIST=0.0001;
    double DEATH_PROB_NORM=0.0001;
    double DEATH_PROB_CONC_SCALE=0.04;
    double RESISTANT_START_PROB=0.5;
    double TREATMENT_PERIOD=2000;
    double TREATMENT_DURATION=250;
    double TREATMENT_DIFF_RATE=0.25;
    double TREATMENT_DECAY=0.99;

    //grid internal objects
    final PDEGrid2D drug;
    final int[]neighborhood=MooreHood(false);
    final int[]divIs=new int[neighborhood.length/2];
    final Random rn;

    //sets up other grid objects
    AdaptiveTherapyExample(int sideLen,Random rn){
        super(sideLen,sideLen,Cell.class,false,false);
        drug=new PDEGrid2D(sideLen,sideLen,false,false);
        this.rn=rn;
    }

    //creates a seed tumor
    public void SetupTumor(int startRad){
        int[] startCoords= CircleHood(false,startRad);
        int[] startIs=new int[startCoords.length/2];
        HoodToIs(startCoords,startIs,xDim/2,yDim/2);
        for (int i : startIs) {
            Cell c = NewAgentSQ(i);
            c.Init(rn.nextDouble() < RESISTANT_START_PROB);
        }
    }

    //runs one model step
    void Step(){
        ReactionDiffusion();
        for (Cell c : this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }

    //draws cell status to provided GuiGridVis
    public static void DrawCells(AdaptiveTherapyExample tissue, GuiGridVis visCells) {
        for (int i = 0; i < visCells.length; i++) {
            Cell c=tissue.GetAgent(i);
            if(c==null){
                //background color (black)
                visCells.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
            }
            else if(c.isResistant){
                //resistant cell color (red)
                visCells.SetPix(i, RGB((double) 1, (double) 0, (double) 0));
            }
            else{
                //sensitive cell color (green)
                visCells.SetPix(i, RGB((double) 0, (double) 1, (double) 0));
            }
        }
    }

    //draws diffusible concentrations to provided GuiGridVis
    public static void DrawDrug(AdaptiveTherapyExample tissue,GuiGridVis visDrug){
        for (int i = 0; i < visDrug.length; i++) {
            visDrug.SetPix(i, HeatMapRGB(tissue.drug.Get(i)));
        }
    }

    //runs a model with a gui for visualization
    public static void RunGui(int sideLen,int timeSteps,int startRad,int visScale,int tickPause){

        //GuiWindow setup
        GuiWindow win=new GuiWindow("Paper Example",true);
        GuiGridVis visCells=new GuiGridVis(sideLen,sideLen,visScale);
        GuiGridVis visDrug=new GuiGridVis(sideLen,sideLen,visScale);
        win.AddCol(0, visCells);
        win.AddCol(1, visDrug);
        win.RunGui();
        TickTimer tt=new TickTimer();

        //tissue setup
        AdaptiveTherapyExample tissue=new AdaptiveTherapyExample(sideLen,new Random());
        tissue.SetupTumor(startRad);

        //main run loop
        for (int i = 0; i < timeSteps; i++) {
            tt.TickPause(tickPause);
            tissue.Step();
            DrawCells(tissue,visCells);
            DrawDrug(tissue,visDrug);
        }
        //gui removed after main loop finishes
        win.Dispose();
    }

    //main function just runs an example model
    public static void main(String[] args) {
        RunGui(SIDE_LEN,TIMESTEPS,START_RAD,VIS_SCALE,TICK_PAUSE);
    }

    //runs a step of reaction diffusion
    void ReactionDiffusion(){
        for (Cell c:this) {
            drug.Mul(c.Isq(),TREATMENT_DECAY);
        }
        if(GetTick()%TREATMENT_PERIOD<TREATMENT_DURATION) {
            drug.Diffusion(TREATMENT_DIFF_RATE, 1);
        }
        else {
            drug.Diffusion(TREATMENT_DIFF_RATE, 0);
        }
    }
}

