package Models.FrameworkLesson;

import Framework.Extensions.SphericalAgent2D;
import Framework.GridsAndAgents.*;
import Framework.Gui.*;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;

import static Framework.Utils.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by bravorr on 6/19/17.
 */

class PolypOL extends AgentGrid2D<PolypCellOL> {
    //Other class members
    final PDEGrid2D resource;
    final ArrayList<PolypCellOL>cellList;
    final Random rn;
    final TickTimer trt;
    final Vis2DOpenGL visPheno;
    final GuiGridVis visRes;
    final FileIO outFile;

    //Initialization parameters
    final double consumptionRateMin=0.01;
    final double consumptionRateMax=0.02;
    final double productionRate=0.008;
    final double mutationRate=0.05;
    final double cellCycleDuration=0.5;
    final double diffusionRate=0.2;
    final double deathProb=0;
    final boolean wrap=false;
    final int tickPause=10;
    final int nSeeds=1;
    final int timeSteps=10000;
    final float[] circleCoords;

    final double cellRad=0.4;
    final double interactionRad=cellRad*2;
    final double forceExp=1;
    final double forceMul=2;
    final double friction=0.2;
    final double divRad=1.0/3.0;
    final double[] divCoords=new double[2];

    //recording values
    int divCt;
    double avgPheno;
    int popCt;

    public static void main(String[] args) {
        int worldSideLen=100;
        int visualizerScale=10;

        GuiWindow gui=new GuiWindow("Resource",true);
        //GuiGridVis visPheno=new GuiGridVis(worldSideLen,worldSideLen,visualizerScale);
        Vis2DOpenGL visPheno=new Vis2DOpenGL("Off Lattice", 1000,1000,worldSideLen,worldSideLen, true);
        GuiGridVis visRes=new GuiGridVis(worldSideLen,worldSideLen,visualizerScale);
        gui.AddCol(1, visRes);
        gui.RunGui();
        FileIO outFile=new FileIO("LessonOut.csv","w");
        PolypOL model=new PolypOL(worldSideLen,visPheno,visRes,outFile);
        model.Run();
    }
    public PolypOL(int worldSideLen, Vis2DOpenGL visPheno,GuiGridVis visRes, FileIO outFile) {
        super(worldSideLen, worldSideLen, PolypCellOL.class);
        this.visPheno=visPheno;
        this.visRes=visRes;
        this.outFile=outFile;
        cellList=new ArrayList<>();
        resource=new PDEGrid2D(worldSideLen,worldSideLen);
        trt=new TickTimer();
        rn=new Random();
        circleCoords=GenCirclePoints((float)cellRad,10);
    }
    public void Run(){
        Reset();
        //typeGrid initialization
        for (int i = 0; i < nSeeds; i++) {
            PolypCellOL c= NewAgentPT(rn.nextDouble()*xDim,rn.nextDouble()*yDim);
            c.Init(rn.nextDouble());
        }
        resource.SetAll(1);
        resource.CurrIntoSwap();
        if(outFile!=null){ outFile.Write("Population,Div Count,GetAvg Pheno\n"); }

        //typeGrid step
        for (int tick = 0; tick < timeSteps; tick++) {
            visPheno.Clear(RGB((float) 0, (float) 0, (float) 0));
            divCt=0; avgPheno=0; popCt=0;
            trt.TickPause(tickPause);
            for (PolypCellOL c : this) {
                c.Step1();
            }
            for (PolypCellOL c : this) {
                c.Step2();
                if(visPheno!=null){
                    visPheno.FanShape((float)c.Xpt(), (float)c.Ypt(), (float) 1, circleCoords,RGB((float) 1, (float)c.phenotype, (float) 0));
                }
            }
            visPheno.Show();
            avgPheno=popCt>0?avgPheno/popCt:0;
            if(outFile!=null){ outFile.Write(popCt+","+divCt+","+avgPheno+"\n"); }
            CleanAgents();
            IncTick();

            resource.AddAll(productionRate);
            resource.BoundAll(0,1);
            resource.Diffusion(diffusionRate,wrap,wrap);
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

class PolypCellOL extends SphericalAgent2D<PolypCellOL,PolypOL> {
    double phenotype;
    double consumptionRate;
    double cellCycleTime;
    double resourceVal;
    public void Init(double phenotype){
        //always called after NewAgentPT
        this.phenotype=phenotype;
        this.consumptionRate=this.phenotype*(G().consumptionRateMax-G().consumptionRateMin)+G().consumptionRateMin;
        cellCycleTime=G().cellCycleDuration;
        this.xVel=0;
        this.yVel=0;
        this.radius=G().cellRad;
    }
//    void SumForces() {
//        G().cellList.clear();
//        G().AgentsInRad(G().cellList, Xpt(), Ypt(), G().interactionRad, G().wrap, G().wrap);
//        for (PolypCellOL c2 : G().cellList) {
//            if (c2 != this) {
//                double xComp = Xdisp(c2, G().wrap);
//                double yComp = Ydisp(c2, G().wrap);
//                double dist = Norm(xComp, yComp);
//                double touchDist = dist - G().interactionRad;
//                if (touchDist <= 0) {
//                    double forceMag = Math.abs(Math.pow(touchDist / G().interactionRad, G().forceExp)) * G().forceMul;
//                    xVel += (xComp / dist) * forceMag;
//                    yVel += (yComp / dist) * forceMag;
//                }
//            }
//        }
//    }
    public void Step1(){
        SumForces(G().interactionRad,G().cellList,(touchDist)->{return Math.abs(Math.pow(touchDist / G().interactionRad, G().forceExp)) * G().forceMul;});
        resourceVal=G().resource.Get(Isq());
    }
    public void Step2(){
        if(G().rn.nextDouble()<G().deathProb||resourceVal<consumptionRate){
            //cell death
            Dispose();
            return;
        }
        G().resource.Add(Isq(),-consumptionRate);
        cellCycleTime-=consumptionRate;
        ApplyFriction(G().friction);
        ForceMove(G().wrap,G().wrap);
        if(cellCycleTime<=0){
            //cell division
            PolypCellOL c=Divide(G().divRad*radius,G().divCoords,G().rn,G().wrap,G().wrap);
            c.Init(phenotype);
            c.Mutate();
            Mutate();
        }
        //pass values to typeGrid for output writing
        G().avgPheno+=phenotype;
        G().popCt++;
    }
//    public void Divide() {
//        double ang=G().rn.nextDouble()*Math.PI*2;
//        double xDiff=Math.cos(ang)*G().divRad;
//        double yDiff=Math.sin(ang)*G().divRad;
//        double x1=Xpt()+xDiff;
//        double y1=Ypt()+yDiff;
//        double x2=Xpt()-xDiff;
//        double y2=Ypt()-yDiff;
//        MoveSafeSQ(x1,y1,G().wrap,G().wrap);
//        PolypCellOL child=G().NewAgentPTSafe(x2,y2,Xpt(),Ypt(),G().wrap,G().wrap);
//        child.Init(phenotype);
//        child.Mutate();
//        Mutate();
//
//    }
    public void Mutate(){
        phenotype= Bound(Gaussian(phenotype,G().mutationRate,G().rn),0,1);
        consumptionRate=this.phenotype*(G().consumptionRateMax-G().consumptionRateMin)+G().consumptionRateMin;
    }
}

