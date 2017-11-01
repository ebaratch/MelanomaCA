package Models.NiccoloMetabolism;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Interfaces.AgentToColorInt;
import Framework.Utils;

import java.util.Random;

import static Models.NiccoloMetabolism.Tissue.*;

class Tissue extends AgentGrid2D<Cell> {
    //"ENUM" CONSTANTS
    static int OXYGEN=0;
    static int GLUCOSE=1;
    static int GLUTAMINE=2;
    static int LACTATE=3;
    static int HYDROGEN=4;
    static int N_CHEM_IN_TISSUE=5;
    static String[] CHEM_NAMES=new String[]{"Oxygen","Glucose","Glutamine","Lactate","Hydrogen"};
    static int BIOMASS=0;
    static int ATP=1;
    static int N_CHEM_IN_CELL=2;

    //MODEL CONSTANTS
    int DIFF_STEPS_PER_CA_STEP=10;
    int STEADY_STATE_DIFF_STEPS=200;



    Random rn=new Random();
    double[] vesselStartConcs=new double[]{1,1,1,1,8};
    double[] cellStartConcs=new double[]{1,1,1};
    PDEGrid2D[] chemicals=new PDEGrid2D[N_CHEM_IN_TISSUE];
    double[] diffRatesUnoccupied=new double[]{0.25,0.25,0.25,0.25,0.25};//diffrates should never go above 0.25
    double[] diffRatesOccupied=new double[]{0.1,0.1,0.1,0.1,0.1};
    double[] diffRates=new double[xDim*yDim];
    Tissue(int xDim,int yDim,int nVessels,double vesselMinSpacing,double startingCellDensity){
        super(xDim,yDim,Cell.class,true,true);
        //setting up diffusion grids
        for (int i = 0; i < chemicals.length; i++) {
            chemicals[i]=new PDEGrid2D(xDim,yDim,true,true);
        }
        //adding vessels to tissue
        SpreadVessels(nVessels,vesselMinSpacing);
        //adding cells to tissue
        AddCells(startingCellDensity);
        IncTick();//tick incremented so that cells that are added to the model will be present for the steady state calculation
        for (int i = 0; i < STEADY_STATE_DIFF_STEPS; i++) {
            DiffStep();
        }
    }

    void SpreadVessels(int nVessels,double minSpacing){
        int[]vesselIs=new int[xDim*yDim];
        for (int i = 0; i < vesselIs.length; i++) {
            vesselIs[i]=i;
        }
        Utils.Shuffle(vesselIs,vesselIs.length,vesselIs.length,rn);
        int vesselCt=0;
        boolean[]invalidPositions=new boolean[xDim*yDim];
        int[]vesselOverlapCircle=Utils.CircleHood(false,minSpacing);
        int[]invalidSquareIs=new int[vesselOverlapCircle.length/2];
        for (int i:vesselIs) {
            if(invalidPositions[i]==false){
                Cell newVessel= NewAgentSQ(i);
                newVessel.Init(true,vesselStartConcs);
                int nPositions= HoodToIs(vesselOverlapCircle,invalidSquareIs,newVessel.Xsq(),newVessel.Ysq());
                for (int j = 0; j < nPositions; j++) {
                    invalidPositions[invalidSquareIs[j]]=true;
                }
                vesselCt++;
            }
            if(vesselCt==nVessels){
                break;
            }
        }
    }

    void AddCells(double startDensity){
        for (int i = 0; i < length; i++) {
            if(rn.nextDouble()<startDensity){
                if(GetAgent(i)==null) {
                    Cell newCell = NewAgentSQ(i);
                    newCell.Init(false, cellStartConcs);
                }
            }
        }
    }

    void Step(){
        DiffStep();
        for (Cell c:this) {
            c.CAStep();
        }
        CleanShuffInc(rn);
    }

    void DiffStep(){
        //Reaction
        for (Cell c:this) {
            c.CellReactionStep();
        }
        //Diffusion
        for (int i = 0; i < N_CHEM_IN_TISSUE; i++) {
            //setting diffusion rates
            for (int j=0;j<diffRates.length;j++) {
                if(this.GetAgent(j)==null){
                    diffRates[j]=diffRatesUnoccupied[i];
                }
                else{
                    diffRates[j]=diffRatesOccupied[i];
                }
            }
            //running diffusion
            chemicals[i].Diffusion(diffRates);
        }
    }
}

class Cell extends AgentSQ2Dunstackable<Tissue> {
    boolean isVessel;
    double[] concs =new double[Math.max(N_CHEM_IN_CELL,N_CHEM_IN_TISSUE)];
    void Init(boolean isVessel,double[] startingConcs){
        this.isVessel=isVessel;
        if(!isVessel) {
            for (int i = 0; i < N_CHEM_IN_CELL; i++) {
                concs[i] = startingConcs[i];
            }
        }
        else{
            for (int i = 0; i < N_CHEM_IN_TISSUE; i++) {
                concs[i]=startingConcs[i];
            }
        }
    }
    void CAStep(){
        //cell division and death
    }

    void CellReactionStep(){
        if(isVessel){
            for (int i = 0; i < N_CHEM_IN_TISSUE; i++) {
                G().chemicals[i].Set(Isq(),concs[i]);
            }
        }
        else{
            //cell reaction aka flux balance analysis
        }
    }
}

public class MetabolismModel {
    static void DrawAll(GuiGridVis[] visChems,GuiGridVis visCells,Tissue t){
        AgentToColorInt<Cell> colorFn = (Cell c)->{
            if(c.isVessel) {
                return Utils.RGB(1, 0, 0);
            }
            else{
                return Utils.RGB(0,1,0);
            }
        };
        visCells.DrawAgents(t, colorFn,Utils.RGB((double) 0, (double) 0, (double) 0));
        for (int i=0;i<visChems.length;i++) {
            visChems[i].DrawGridDiff(t.chemicals[i],(val)->{
                return Utils.HeatMapRBG(Utils.RescaleMinToMax(val, (double) 0, (double) 1));
            });
        }
    }
    static void RunModel(int xDim,int yDim,int nVessels,int vesselMinSpacing,int runTicks,boolean guiOn,int gridsPerRow,int msPerTick,double startingDensity){
        TickTimer trt=new TickTimer();
        //Setting up gui
        GuiWindow win=new GuiWindow("StaticModel Vis",true,guiOn);
        GuiGridVis[] visChems=new GuiGridVis[N_CHEM_IN_TISSUE];
        for (int i = 0; i < visChems.length; i++) {
            visChems[i]=new GuiGridVis(xDim,yDim,5,guiOn);
            win.AddCol(i%gridsPerRow, new GuiLabel(CHEM_NAMES[i]));
            win.AddCol(i%gridsPerRow, visChems[i]);
        }
        GuiGridVis visCells=new GuiGridVis(xDim,yDim,10,2,1,guiOn);//visCells is 2x larger, so it takes up 2 cols
        win.AddCol(0, new GuiLabel("Agents",2,1));
        win.AddCol(0, visCells);
        win.RunGui();

        Tissue t=new Tissue(xDim,yDim,nVessels,vesselMinSpacing,startingDensity);
        for (int i = 0; i < runTicks; i++) {
            trt.TickPause(msPerTick);
            t.Step();
            DrawAll(visChems,visCells,t);
        }
        win.Dispose();//closes the window
    }

    public static void main(String[] args) {
        RunModel(20,20,10,2,100,true,4,100,1);
    }
}
