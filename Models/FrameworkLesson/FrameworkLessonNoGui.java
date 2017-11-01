package Models.FrameworkLesson;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.*;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.Random;

import static Framework.Utils.Bound;
import static Framework.Utils.Gaussian;
import static Framework.Utils.MooreHood;

/**
 * Created by bravorr on 6/19/17.
 */

class Polyp extends AgentGrid2D<PolypCell> {
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
    final double consumptionRateMin=0.01;
    final double consumptionRateMax=0.02;
    final double productionRate=0.008;
    final double mutationRate=0.05;
    final double cellCycleDuration=0.5;
    final double diffusionRate=0.25;
    final double deathProb=0;
    final boolean wrap=false;
    final int tickPause=10;
    final int nSeeds=1;
    final int timeSteps=10000;

    //recording values
    int divCt;
    double avgPheno;
    int popCt;

    public static void main(String[] args) {
        int worldSideLen=200;
        int visualizerScale=5;

        GuiWindow gui=new GuiWindow("StaticModel Run",true);
        GuiGridVis visPheno=new GuiGridVis(worldSideLen,worldSideLen,visualizerScale);
        gui.AddCol(0, new GuiLabel("Phenotype"));
        gui.AddCol(0, visPheno);
        GuiGridVis visRes=new GuiGridVis(worldSideLen,worldSideLen,visualizerScale);
        gui.AddCol(1, new GuiLabel("Resource"));
        gui.AddCol(1, visRes);
        gui.RunGui();
        FileIO outFile=new FileIO("LessonOut.csv","w");
        Polyp model=new Polyp(worldSideLen,visPheno,visRes,outFile);
        model.Run();
    }
    public Polyp(int worldSideLen,GuiGridVis visPheno, GuiGridVis visRes, FileIO outFile) {
        super(worldSideLen, worldSideLen, Models.FrameworkLesson.PolypCell.class);
        this.visPheno=visPheno;
        this.visRes=visRes;
        this.outFile=outFile;
        resource=new PDEGrid2D(worldSideLen,worldSideLen);
        mooreHood =MooreHood(false);
        divIs=new int[mooreHood.length/2];
        trt=new TickTimer();
        rn=new Random();
    }
    public void Run(){
        Reset();
        //typeGrid initialization
        int[] ret= Utils.GenIndicesArray(length);
        Utils.Shuffle(ret, length, nSeeds, rn);
        int[] seedIs= ret;
        for (int i = 0; i < nSeeds; i++) {
            Models.FrameworkLesson.PolypCell c = NewAgentSQ(seedIs[i]);
            c.Init(rn.nextDouble());
        }
        resource.SetAll(1);
        if(outFile!=null){ outFile.Write("Population,Div Count,GetAvg Pheno\n"); }

        //typeGrid step
        for (int tick = 0; tick < timeSteps; tick++) {
            divCt=0; avgPheno=0; popCt=0;
            trt.TickPause(tickPause);
            for (Models.FrameworkLesson.PolypCell c : this) {
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
                        visRes.SetPix(x, y,Utils.HeatMapRGB(resource.Get(x,y)));
                    }
                }
            }
        }
        if(outFile!=null){ outFile.Close(); }
    }
}

class PolypCell extends AgentSQ2Dunstackable<Polyp> {
    double phenotype;
    double consumptionRate;
    double cellCycleTime;
    public void Init(double phenotype){
        //always called after NewAgentPT
        this.phenotype=phenotype;
        this.consumptionRate=this.phenotype*(G().consumptionRateMax-G().consumptionRateMin)+G().consumptionRateMin;
        cellCycleTime=G().cellCycleDuration;
        if(G().visPheno!=null){
            G().visPheno.SetPix(Xsq(), Ysq(),Utils.RGB((double) 1, (double) (float) phenotype, (double) 0));
        }
    }
    public void Step(){
        double myResource=G().resource.Get(Isq());
        if(G().rn.nextDouble()<G().deathProb||myResource<consumptionRate){
            //cell death
            if(G().visPheno!=null){
                G().visPheno.SetPix(Xsq(), Ysq(),Utils.RGB((double) 0, (double) 0, (double) 0));
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
            Models.FrameworkLesson.PolypCell child=G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivIs)]);
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

