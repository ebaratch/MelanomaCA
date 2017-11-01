package Models.BaileyMacrophage;


import Framework.Extensions.MarksModelCell;
import Framework.Extensions.MarksModelGrid;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentPT2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
//import Framework.Gui.Vis2DOpenGL;
import Framework.Interfaces.AgentToColorInt;
import Framework.Tools.FileIO;
import Framework.Utils;

import java.util.Random;

import static Framework.Utils.*;
import static Models.BaileyMacrophage.MacroGrid.*;

/**
 * Created by bravorr on 6/28/17.
 */

class MacroGrid extends AgentGrid2D<MacroCell> {
    public static boolean phEffect = false; ///Does pH affect phenotype?
    public static int RANDOM_SEED = 1;
    public static boolean GuiOn = true;
    public static String f_out = "testOut.csv";
    public static String dst_dir = "./";
    final static boolean RUN_FROM_CMD_LINE = true;

    final static int NO_CELL=0;
    final static int NECROTIC_CELL=-1;
    final static int TUMOR_CELL=1;
    final static int DIGESTING_CAP=5;
    final static double MIGRATE_SPEED=2;
    final static double DIGESTION_TIME=1;
    final static double DIGESTION_STEP=1;

    final static double CCL2_DIFF_RATE=0.25;
    final static double CCL2_MAX_DIFF=500;
    final static double CCL2_DECAY=.982;

    final static double CCL2_DISSOCIATION_CONSTANT=10000;
    final static double CCL2_HILL_CONSTANT=1;

    final static double TGFB1_DIFF_RATE=0.25;
    final static double TGFB1_MAX_DIFF=500;
    final static double TGFB1_DECAY=0.99;

    final static double TGFB1_DISSOCIATION_CONSTANT=800;
    final static double TGFBI_HILL_CONSTANT=1;

    final static int MACROPHAGE_STARTING_POP=100;
    final static int STEP_TOTAL=1000;
    final static int CHEMOTAXIS_STRENGTH = 1000 ; ///NOTE: Larger number = weaker
    double[]coordScratch=new double[2];


    PDEGrid2D ccl2DiffGrid;
    PDEGrid2D tgfb1DiffGrid;




    MarkGrid cells;
    Random rn;
    final static float[] drawCirclePts= GenCirclePoints(1,10);
    MacroGrid(int x, int y, int nStartingPop, MarkGrid cells, Random rn){
        /*
        Place macrophages randomly in world, and also makes diffusion grids
         */
        super(x,y,MacroCell.class);
        this.rn=rn;
        this.cells=cells;
        for (int i = 0; i < nStartingPop ; i++) {
            double xPos = rn.nextDouble() * xDim;
            double yPos = rn.nextDouble() * yDim;
            NewMacroCell(xPos, yPos);
        }
        ccl2DiffGrid= new PDEGrid2D(x, y);
        tgfb1DiffGrid= new PDEGrid2D(x, y);
    }
    public MacroCell NewMacroCell(double x, double y) {
        MacroCell ret = NewAgentPT(x, y);
        ret.Init();
        return ret;
    }

//    public void MacroDraw(Vis2DOpenGL vis){
//        vis.Clear(0,0,0);
//        for (MacroCell c : this) {
//            float gColor=(float)c.scaledArg1Express;
//            float rColor=(float)c.scaledCcrl2Express;
//            float bColor=(float)0;
//
//
//            vis.FanShape((float)c.Xpt(),(float)c.Ypt(),1,drawCirclePts, rColor, gColor, bColor);
//        }
//        vis.Show();
//    }
    public void Step(){
        for (MacroCell c : this) {
            c.Step();
        }
        CleanShuffInc(rn);
        CytokineDiffLoop();
    }

    public void CytokineDiffLoop() {
        int DiffCount = 0;
        double ccl2Diff = 0;
        double tgfbDiff = 0;
        do {
            for (MacroCell c : this) {
                if (c.firstBlood) {
                    ccl2DiffGrid.Add(c.Xsq(), c.Ysq(), c.ccl2Express);
                    tgfb1DiffGrid.Add(c.Xsq(), c.Ysq(), c.tgfb1Express);
                }
            }
            for (int i = 0; i < ccl2DiffGrid.length; i++) {
                ccl2DiffGrid.Set(i,ccl2DiffGrid.Get(i)*CCL2_DECAY);
            }
            for (int i = 0; i < tgfb1DiffGrid.length; i++) {
                tgfb1DiffGrid.Set(i,tgfb1DiffGrid.Get(i)*TGFB1_DECAY);
            }
            ccl2Diff = ccl2DiffGrid.MaxDifSwap();
            tgfbDiff = tgfb1DiffGrid.MaxDifSwap();
            ccl2DiffGrid.Diffusion(CCL2_DIFF_RATE);
            tgfb1DiffGrid.Diffusion(TGFB1_DIFF_RATE);

            DiffCount++;
        }
        while (ccl2Diff > CCL2_MAX_DIFF || tgfbDiff > TGFB1_MAX_DIFF);
    }
}

class MacroCell extends AgentPT2D<MacroGrid> {
    int[] cellsBeingDigested=new int[DIGESTING_CAP];
    double[] digestionTimes=new double[DIGESTING_CAP];
    boolean firstBlood; ///Have macrophages detected a cell of interest?
    double ccl2Express;
    double ccrl2Express;
    double fcer1gExpress;
    double scaledFcer1gExpress;
    double mrc1Express;
    double scaledMrc1Express;
    double tgfb1Express;
    double tgfbr1Express;
    double vegfaExpress;
    double arg1Express;
    double scaledArg1Express;
    double scaledCcrl2Express;


    double ph;
    double inflam;
    double antiInflam;
    double scaledInflam;
    double scaledAntiInflam;
    int tumor;
    double scaledTumor;
    int necrotic;
    double scaledNecrotic;
    double envCond;

    int iOpen=0; ///Position in digestion array that is open?

    public void Init() {

//        SetExpression(); ///TODO CHECK IF SET EXPRESSION SHOULD BE HERE?
        firstBlood = false;
        iOpen = 0;
        for (int i = 0; i < DIGESTING_CAP; i++) {
            cellsBeingDigested[i] = 0;
            digestionTimes[i] = 0;
        }
    }


    public void SetExpression(){
        if(phEffect){
            ph= ProtonsToPh(G().cells.protons.Get(Isq()));
        }
        else{
            ph = 7.4;
        }

        scaledTumor= tumor/5d;
        scaledNecrotic= necrotic/5d;

        ///NOTE Only affected by cytokines if digesting cells
        if(scaledTumor == 0 && scaledNecrotic == 0){
            scaledInflam = 0;
            scaledAntiInflam = 0;
            firstBlood = false ;
        }
        else{

            scaledInflam= ScaleData(inflam, 0, 4000, 0, 1);
            scaledAntiInflam = ScaleData(antiInflam, 0, 3500, 0, 1);
        }


        envCond= (-0.5*scaledInflam)+(0.5*scaledAntiInflam)+(-0.5*(scaledTumor))+(0.5*(scaledNecrotic));
        ///TODO PUT THESE COEF AT THE TOP OF CLASS
        ccl2Express= PredictExpression(-140893, 22274, 121063, -18943 );
        ccrl2Express= PredictExpression(-383.1, 225.3, 449.0, -228.5 );
        tgfb1Express= PredictExpression(3465.40, -62.42, 6138.16, -796.27 );
        tgfbr1Express= PredictExpression(-6041.1, 1246.0, 1788.3, -210.3 );
        fcer1gExpress= PredictExpression(11402.7, -702.9, -4451.6, 290.0);
        mrc1Express= PredictExpression(22419, -2523, 19382, -2195);
        arg1Express= PredictExpression(126004, -15936, 120034, -15150);

        scaledCcrl2Express= ScaleData(ccrl2Express, 0, 2500, 0, 1);
        scaledArg1Express= ScaleData(arg1Express, 0, 50000, 0, 1);
        scaledFcer1gExpress= ScaleData(fcer1gExpress, 0, 10000, 0, 5);
        scaledMrc1Express= ScaleData(mrc1Express, 0, 10000, 0, 5);
    }

    public double PredictExpression(double intercept, double phCoef, double envCondCoef, double interactionCoef){
        return intercept+(phCoef*ph)+(envCondCoef*envCond)+(interactionCoef*ph*envCond);

    }


    public double ScaleData(double x, double inMin, double inMax, double outMin, double outMax){
        return ((outMax - outMin)*(x-inMin))/(inMax - inMin) + outMin;
    }

    public void Step(){
        /*
        Move -> Continue digesting current cells -> Eat new cells -> SetBlock expression -> Bind cytokines
         */
        Chemotaxis(G().ccl2DiffGrid.GradientX(Xsq(), Ysq()), G().ccl2DiffGrid.GradientY(Xsq(), Ysq()), G().coordScratch, 10);
        for (int i = 0; i < DIGESTING_CAP; i++) {
            if (digestionTimes[i] != 0) {
                digestionTimes[i] -= DIGESTION_STEP;
            } else {
                if (digestionTimes[i] == 0) {
                    int cellType;
                    cellType = cellsBeingDigested[i];
                    if (cellType == TUMOR_CELL) {
                        this.tumor = this.tumor - 1;
                        cellsBeingDigested[i] = 0;
                    } else if (cellType == NECROTIC_CELL) {
                        this.necrotic = this.necrotic - 1;
                        cellsBeingDigested[i] = 0;
                    }
                }
            }
        }

        Digest();
        BindInflam();
        BindAntiInflam();
        SetExpression();
    }


    public void BindInflam(){
        double ccl2Concentration;
        double ccl2Bound;

        ccl2Concentration= G().ccl2DiffGrid.Get(Isq());
        ccl2Bound= HillEqn(ccl2Concentration, CCL2_DISSOCIATION_CONSTANT, CCL2_HILL_CONSTANT)*ccrl2Express;
        if (ccl2Bound > ccl2Concentration){
            ccl2Bound= ccl2Concentration;
        }
        G().ccl2DiffGrid.Add(Isq(), -ccl2Bound);
        this.inflam= ccl2Bound;
    }

    public void BindAntiInflam(){
        double tgfb1Concentration;
        double tgfb1Bound;
        tgfb1Concentration= G().tgfb1DiffGrid.Get(Isq());
        tgfb1Bound= (HillEqn(tgfb1Concentration, TGFB1_DISSOCIATION_CONSTANT, TGFBI_HILL_CONSTANT))*tgfbr1Express;
        if (tgfb1Bound > tgfb1Concentration){
            tgfb1Bound= tgfb1Concentration;
        }
        G().tgfb1DiffGrid.Add(Isq(), -tgfb1Bound);
        this.antiInflam= tgfb1Bound;
    }


    public MarkGrid M(){
        return G().cells;
    }



    public boolean StartDigesting(MarkCell c){
        /*
        Determine cell type being digested and remove from world
         */
        if(digestionTimes[iOpen]<=0){
            if (c.isCancer) {
                this.tumor++;
                cellsBeingDigested[iOpen] = TUMOR_CELL;
                if(firstBlood==false) {
                    firstBlood = true;
                }
            }
            else {
                this.necrotic++;
                cellsBeingDigested[iOpen] = NECROTIC_CELL;
            }
            digestionTimes[iOpen]=DIGESTION_TIME;
            iOpen=(iOpen+1)%DIGESTING_CAP;
            c.Dispose();
            return true;
        }
        return false;
    }

    public void Digest(){
        /*
        Check cell type -> consume if space available
         */
        MarkCell c=M().GetAgent(Xsq(),Ysq());
        if(c!=null){
            if((!c.IsAlive()&&necrotic<scaledMrc1Express)||(c.isCancer&&tumor<scaledFcer1gExpress)){
                StartDigesting(c);
            }
        }
    }

//    public void Migrate(){
//        double angle=G().rn.nextDouble()*Math.PI*2;
//        double y=Math.sin(angle)*MIGRATE_SPEED;
//        double x=Math.cos(angle)*MIGRATE_SPEED;
//        MoveSafeSQ(Xpt()+x,Ypt()+y,false,false);
//    }

//    double[] xAndY= new double[2];
//    double[] weights= new double[2];
//    public void Ccl2Chemtaxis(){
//        try {
//            double xWeight=G().ccl2DiffGrid.GradientX2D(Xsq(),Ysq());
//            double yWeight=G().ccl2DiffGrid.GradientY2D(Xsq(),Ysq());
//
////            double sumX=(G().ccl2DiffGrid.GetCurr(Xsq()-1, Ysq())+G().ccl2DiffGrid.GetCurr(Xsq()+1,Ysq())+G().ccl2DiffGrid.GetCurr(Xsq(),Ysq()));
////            double sumY=(G().ccl2DiffGrid.GetCurr(Xsq(), Ysq()-1)+G().ccl2DiffGrid.GetCurr(Xsq(),Ysq()+1)+G().ccl2DiffGrid.GetCurr(Xsq(),Ysq()));
////
////            if (sumX!=0&&sumY!=0) {
////                xWeight= xWeight/sumX;
////                yWeight= yWeight/sumY;
////            } else {
////                xWeight=0;
////                yWeight=0;
////            }
//
//            weights[0] = xWeight;
//            weights[1] = yWeight;
//        } catch (Exception e) {
//            System.out.println("un weighted migration because on edge");
//            weights[0] = 0;
//            weights[1] = 0;
//        }

//        weights[0]=0;
//        weights[1]=0;
//        RandomWeightedPointOnCircle(MIGRATE_SPEED, 0.01, G().rn, weights,xAndY);
//        if (!Double.isNaN(xAndY[0]) && !Double.isNaN(xAndY[1])) {
//            MoveSafeSQ(Xpt()+xAndY[0],Ypt()+xAndY[1],false,false);
//        }
//    }

    public void Chemotaxis(double gradX,double gradY,double[] out,double moveRad){
        gradX=gradX/ CHEMOTAXIS_STRENGTH; ///TODO PLAY WITH DENOM. TOO MUCH CHEMOTAXIS
        gradY=gradY/ CHEMOTAXIS_STRENGTH;

        double gradMag=Norm(gradX,gradY);
//        System.out.println(gradMag);
        double maxCenterDisp=moveRad/4;
        if(gradMag>maxCenterDisp){
            //forcing movement circle center to be at most maxCenterDisp away
            gradX=gradX*maxCenterDisp/gradMag;
            gradY=gradY*maxCenterDisp/gradMag;
            gradMag=maxCenterDisp;
        }
        double moveCircleRad=moveRad-gradMag;
        RandomPointInCircle(moveCircleRad, G().coordScratch, G().rn);
        //adding center of circle and point coordinates to x and y position to compute destination
        double nextX=Xpt()+gradX+G().coordScratch[0];
        double nextY=Ypt()+gradY+G().coordScratch[1];
        MoveSafePT(nextX,nextY);
    }


}

class MarkGrid extends MarksModelGrid<MarkCell> {

    MacroGrid macros;
    public MarkGrid(int x, int y, Random rn) {
        super(x, y, MarkCell.class,rn);
        macros=new MacroGrid(x,y,MACROPHAGE_STARTING_POP,this,rn);
        //OXYGEN_DIFF_RATE =500f*DIFF_TIME_STEP*1.0f/(GRID_SIZE*GRID_SIZE);

    }

    public void MyStep(){
        DiffLoop(false);
        for (MarksModelCell c : this) {
            c.DefaultStep();
        }

        CleanShuffInc(rn);
        Angiogenesis();

    }

}

class MarkCell extends MarksModelCell<MarkCell,MarkGrid> {
}

public class MacrophageModel {
    public static void UseArgs(String[] args){
        if(args.length==0){
            return;
        }
        MacroGrid.phEffect=Boolean.parseBoolean(args[0]);
        MacroGrid.RANDOM_SEED=Integer.parseInt(args[1]);
        MacroGrid.dst_dir=args[2];
        MacroGrid.f_out=args[3];
        Utils.MakeDirs(MacroGrid.dst_dir);
        MacroGrid.f_out = MacroGrid.dst_dir + MacroGrid.f_out;
        MacroGrid.GuiOn = false;
        System.out.println("Modeling pH Effect: " + MacroGrid.phEffect +
        " Sim #: " + MacroGrid.RANDOM_SEED +
        " File Out: " + MacroGrid.f_out);
    }
    public static void main(String[] args) {
        System.out.println(args.length);
        System.out.println("args: ["+ Utils.ArrToString(args,",")+"]");
        UseArgs(args);

        int x=100;
        int y=100;
        GuiWindow win=new GuiWindow("testDisp",true,GuiOn);
        GuiGridVis visCells=new GuiGridVis(x,y,2,GuiOn);
        GuiGridVis visO2=new GuiGridVis(x,y,2,GuiOn);
        GuiGridVis visAcid=new GuiGridVis(x,y,2, GuiOn);
        GuiGridVis visGlu=new GuiGridVis(x,y,2, GuiOn);
        GuiGridVis visPheno=new GuiGridVis(x,y,4,1,3, GuiOn);
        GuiGridVis visCcl2=new GuiGridVis(x,y,2, GuiOn);
        GuiGridVis visTGFb=new GuiGridVis(x,y,2, GuiOn);
        GuiGridVis visActive=new GuiGridVis(x,y,2, GuiOn);
//        Vis2DOpenGL visMacro=new Vis2DOpenGL(200,200,x,y,"Macrophages",GuiOn);
        win.AddCol(0, new GuiLabel("Cells", GuiOn));
        win.AddCol(0, visCells);
        win.AddCol(0, new GuiLabel("Oxygen", GuiOn));
        win.AddCol(0, visO2);
        win.AddCol(1, new GuiLabel("pH", GuiOn));
        win.AddCol(1, visAcid);
        win.AddCol(1, new GuiLabel("Glucose", GuiOn));
        win.AddCol(1, visGlu);
        win.AddCol(2, new GuiLabel( "red: acid resist green: glycolytic", GuiOn));
        win.AddCol(2, visPheno);
        win.AddCol(3, new GuiLabel("CCL2", GuiOn));
        win.AddCol(3, visCcl2);
        win.AddCol(3, new GuiLabel("TGFb", GuiOn));
        win.AddCol(3, visTGFb);
        win.AddCol(4, visActive);
        win.RunGui();

        MarkGrid g=new MarkGrid(x,y,new Random(RANDOM_SEED));
        g.FillGrid(0.8);
        g.CreateTumor(5,g.xDim/2,g.yDim/2);
        g.InitDiffusibles();
        FileIO out=new FileIO(MacroGrid.f_out,"w");
        out.Write("AvgPH,AvgArg1,AvgCCRL2,tumorCt,necroCt\n");
        for (int i = 0; i < STEP_TOTAL; i++) {
            win.TickPause(100);
            g.MyStep();
            g.macros.Step();
            System.out.println(g.GetTick());
            g.DrawCells(visCells);
            g.DrawMicroEnvHeat(visO2,false,false,true);
            g.DrawMicroEnvHeat(visAcid,false,true,false);
            g.DrawMicroEnvHeat(visGlu,true,false,false);
            g.DrawPheno(visPheno);
//            g.macros.MacroDraw(visMacro);
            visCcl2.DrawGridDiff(g.macros.ccl2DiffGrid,(val)-> HeatMapGBR(0,450000,val));
            visCcl2.DrawGridDiff(g.macros.tgfb1DiffGrid,(val)-> HeatMapBGR(0,400000,val));
            AgentToColorInt<MacroCell> colorFn = (MacroCell c)->{
                if(c.firstBlood){
                    return RGB(1,1,0);
                }
                else{
                    return RGB(1,0,0);
                }
            };
            visActive.DrawAgents(g.macros, colorFn, RGB((double) 0, (double) 0, (double) 0));

            //Getting Values of interest for the timestep
            int tumorCt=0;
            int necroCt=0;
            for (MarkCell c : g) {
                if(c.IsAlive()&&c.isCancer){
                    tumorCt++;
                }
                if(!c.IsAlive()){
                    necroCt++;
                }
            }
            double avgPH= ProtonsToPh(g.protons.GetAvg());
            double arg1Sum=0;
            double ccrl2Sum=0;
            for(MacroCell c:g.macros){
                arg1Sum+=c.arg1Express;
                ccrl2Sum+=c.ccrl2Express;
            }
            out.Write(avgPH + "," + arg1Sum + "," + ccrl2Sum + "," + tumorCt + "," + necroCt + "\n");
        }
        out.Close();
        win.Dispose();
    }

}



