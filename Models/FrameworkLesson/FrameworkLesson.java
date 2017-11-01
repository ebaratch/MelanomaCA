package Models.FrameworkLesson;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.*;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;

import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by bravorr on 6/18/17.
 */

public class FrameworkLesson {
    public static void main(String[] args) {
        GuiWindow win=new GuiWindow("Menu",true);
        //first column of options
        win.AddCol(0, new GuiDoubleField("Consumption Rate Min",0.01,0,1));
        win.AddCol(0, new GuiDoubleField("Consumption Rate GetMax",0.02,0,1));
        win.AddCol(0, new GuiDoubleField("Replenishment Rate",0.008,0,1));
        win.AddCol(0, new GuiDoubleField("Mutation Rate",0.05,0,1));
        win.AddCol(0, new GuiDoubleField("Diffusion Rate",0.25,0,1));
        win.AddCol(0, new GuiDoubleField("Cell Cycle Duration",0.5,0.1,100));
        win.AddCol(0, new GuiBoolField("Wrap Around",false));
        win.AddCol(0, new GuiIntField("World Side Length",200,10,10000));
        win.AddCol(0, new GuiIntField("Seed Cells",1,1,100));
        win.AddCol(0, new GuiDoubleField("Death Prob",0,0,1));

        //second column of options
        win.AddCol(1, new GuiIntField("Time Steps",10000,10,1000000));
        win.AddCol(1, new GuiIntField("RunStepSeq Rate (ms)",10,0,1000));
        win.AddCol(1, new GuiIntField("Visualizer Scale",5,1,10));
        win.AddCol(1, new GuiBoolField("Show Phenotype",true));
        win.AddCol(1, new GuiBoolField("Show Resource",true));
        win.AddCol(1, new GuiBoolField("Record Population",true));
        win.AddCol(1, new GuiFileChooserField("Output File","LessonOut.csv"));
        win.AddCol(1, new GuiButton("RunModel",true,(e) -> {
            win.GreyOut(true);
            FileIO outFile=null;
            GuiGridVis visPheno=null;
            GuiGridVis visRes=null;
            GuiWindow visWin=null;
            boolean bVisPheno=win.GetBool("Show Phenotype");
            boolean bVisRes=win.GetBool("Show Resource");
            int worldSideLen=win.GetInt("World Side Length");
            int viewerScale=win.GetInt("Visualizer Scale");
            final boolean[] running = {true};
            if(win.GetBool("Record Population")){
                outFile=new FileIO(win.GetString("Output File"),"w");
            }
            if(bVisPheno||bVisRes){
                visWin=new GuiWindow("StaticModel Run",false,(ex)->{
                  running[0] =false;
                }, false);
                if(bVisPheno){
                    visPheno=new GuiGridVis(worldSideLen,worldSideLen,viewerScale);
                    visWin.AddCol(0, new GuiLabel("Phenotype"));
                    visWin.AddCol(0, visPheno);
                }
                if(bVisRes){
                    visRes=new GuiGridVis(worldSideLen,worldSideLen,viewerScale);
                    visWin.AddCol(1, new GuiLabel("Resource"));
                    visWin.AddCol(1, visRes);
                }
                visWin.RunGui();
            }
            Polyp2D polyp=new Polyp2D(worldSideLen,win.GetDouble("Consumption Rate Min"),win.GetDouble("Consumption Rate GetMax"),win.GetDouble("Replenishment Rate"),win.GetDouble("Mutation Rate"),win.GetDouble("Cell Cycle Duration"),win.GetDouble("Diffusion Rate"),win.GetInt("RunStepSeq Rate (ms)"),win.GetBool("Wrap Around"),visPheno,visRes,win.GetDouble("Death Prob"),outFile, running);
            polyp.Run(win.GetInt("Seed Cells"),win.GetInt("Time Steps"));
            if(visWin!=null){ visWin.Dispose(); }
            win.GreyOut(false);
            }));
        win.RunGui();
    }
}

class PolypCell2D extends AgentSQ2Dunstackable<Polyp2D> {
    double phenotype;
    double consumptionRate;
    double cellCycleTime;
    public void Init(double phenotype){
        //always called after NewAgentPT
        this.phenotype=phenotype;
        this.consumptionRate=this.phenotype*(G().consumptionRateMax-G().consumptionRateMin)+G().consumptionRateMin;
        cellCycleTime=G().cellCycleDuration;
        if(G().visPheno!=null){
            G().visPheno.SetPix(Xsq(), Ysq(), RGB((double) 1, (double) (float) phenotype, (double) 0));
        }
    }
    public void Step(){
        double myResource=G().resource.Get(Isq());
        if(G().rn.nextDouble()<G().deathProb||myResource<consumptionRate){
            //cell death
            if(G().visPheno!=null){
                G().visPheno.SetPix(Xsq(), Ysq(), RGB((double) 0, (double) 0, (double) 0));
            }
            Dispose();
            return;
        }
        G().resource.Add(Isq(),-consumptionRate);
        cellCycleTime-=consumptionRate;
        if(cellCycleTime<=0){
            //cell division
            if(Divide()){
                cellCycleTime=G().cellCycleDuration;
                G().divCt++;
            }
        }
        //pass values to typeGrid for output writing
        G().avgPheno+=phenotype;
        G().popCt++;
    }
    public boolean Divide() {
        int nIs = G().HoodToIs(G().mooreHood, G().divIs, Xsq(), Ysq(), G().wrap, G().wrap);
        int nDivIs = 0;
        for (int i = 0; i < nIs; i++) {
            //find open squares
            if (G().GetAgent(G().divIs[i]) == null) {
                G().divIs[nDivIs] = G().divIs[i];
                nDivIs++;
            }
        }
        if (nDivIs > 0) {
            //create daugther cell if an empty square exists
            PolypCell2D child=G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivIs)]);
            child.Init(phenotype);
            child.Mutate();
            Mutate();
            phenotype= Bound(Gaussian(phenotype,G().mutationRate,G().rn),0,1);
            return true;
        }
        return false;
    }
    public void Mutate(){
        phenotype= Bound(Gaussian(phenotype,G().mutationRate,G().rn),0,1);
    }
}

class Polyp2D extends AgentGrid2D<PolypCell2D> {
    //Other class members
    final PDEGrid2D resource;
    final int[] mooreHood;
    final int[] divIs;
    final Random rn;
    final TickTimer trt;
    final GuiGridVis visPheno;
    final GuiGridVis visRes;
    final FileIO outFile;

    //Initialization parameters
    final double consumptionRateMin;
    final double consumptionRateMax;
    final double productionRate;
    final double mutationRate;
    final double cellCycleDuration;
    final double diffusionRate;
    final double deathProb;
    final boolean wrap;
    final int tickPause;

    //recording values
    int divCt;
    double avgPheno;
    int popCt;

    final boolean[] running;
    public Polyp2D(int worldSideLen, double consumptionRateMin, double consumptionRateMax, double productionRate, double mutationRate, double cellCycleDuration,double diffusionRate,int tickPause,boolean wrap, GuiGridVis visPheno, GuiGridVis visRes,double deathProb, FileIO outFile,boolean[] running) {
        super(worldSideLen, worldSideLen, PolypCell2D.class);
        this.consumptionRateMin=consumptionRateMin;
        this.consumptionRateMax=consumptionRateMax;
        this.productionRate=productionRate;
        this.mutationRate=mutationRate;
        this.cellCycleDuration=cellCycleDuration;
        this.wrap=wrap;
        this.visPheno=visPheno;
        this.visRes=visRes;
        this.outFile=outFile;
        this.diffusionRate=diffusionRate;
        this.tickPause=tickPause;
        this.deathProb=deathProb;
        resource=new PDEGrid2D(worldSideLen,worldSideLen);
        mooreHood =MooreHood(false);
        divIs=new int[mooreHood.length/2];
        trt=new TickTimer();
        rn=new Random();
        this.running=running;
    }
    public void Run(int nSeeds,int duration){
        Reset();
        //typeGrid initialization
        int[] ret= GenIndicesArray(length);
        Shuffle(ret, length, nSeeds, rn);
        int[] seedIs= ret;
        for (int i = 0; i < nSeeds; i++) {
            PolypCell2D c = NewAgentSQ(seedIs[i]);
            c.Init(rn.nextDouble());
        }
        resource.SetAll(1);
        if(outFile!=null){ outFile.Write("Population,Div Count,GetAvg Pheno\n"); }

        //typeGrid step
        for (int tick = 0; tick < duration; tick++) {
            divCt=0; avgPheno=0; popCt=0;
            if(!running[0]){ break; }
            trt.TickPause(tickPause);
            for (PolypCell2D c : this) {
                c.Step();
            }
            avgPheno=popCt>0?avgPheno/popCt:0;
            if(outFile!=null){ outFile.Write(popCt+","+divCt+","+avgPheno+"\n"); }
            CleanShuffInc(rn);
            resource.AddAll(productionRate);
            resource.Diffusion(diffusionRate,wrap,wrap);
            resource.BoundAll(0,1);
            if(visRes!=null){
                for (int x = 0; x < xDim; x++) {
                    for (int y = 0; y < yDim; y++) {
                        visRes.SetPix(x, y, HeatMapRGB(resource.Get(x,y)));
                    }
                }
            }
        }
        if(outFile!=null){ outFile.Close(); }
    }
}
