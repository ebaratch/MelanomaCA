package Models.JeffreyModels.BirthDeath;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GifMaker;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Interfaces.TreatableTumor;
import Framework.Tools.FileIO;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import static Framework.Utils.*;

class Cell extends AgentSQ2Dunstackable<BirthDeath> {
    int k;

    void Init(int k0){
        k = k0;
    }
    void Mutate(){
        if((G().rn.nextDouble()<G().GENE_MUT_RATE) && (k < G().KMAX)){
            k++;
        }
    }

    double GetBirthProbability(){
        return G().BIRTH_RATE;
        //return G().DEATH_RATE*Math.pow((1+G().S),(double)k);
    }

    double GetDeathProbability(){
        return G().BIRTH_RATE*Math.pow((1-G().S),(double)k);
        //return G().DEATH_RATE;
    }



    Cell Divide(){
        int nDivOptions=G().HoodToEmptyIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        if(nDivOptions==0){ return null; }
        Cell daughter=G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivOptions)]);
        daughter.Init(k);
        daughter.Mutate();
        Mutate();
        return daughter;
    }
    void Step(){

        // check if birth event
        if(G().rn.nextDouble()<GetBirthProbability()){
            Divide();
        }

        if(G().rn.nextDouble()<GetDeathProbability()){
            Dispose();
            return;
        }
    }
}

public class BirthDeath extends AgentGrid2D<Cell> implements TreatableTumor,Serializable{

    // model params (default values)
    public double S = 0.1;
    public double BIRTH_RATE = 0.5;
    public double GENE_MUT_RATE = 0.001;
    public int K0 = 1;
    public int KMAX = 10;
    public static int BLACK = RGB(0,0,0);

    // neighborhoods
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];
    Random rn=new Random();

    BirthDeath(int sideLen, int r0){
        super(sideLen,sideLen, Cell.class,true,true);

        // circle hoods
        int[]circleHood=CircleHood(true, r0);
        int[]circleArrayIs=new int[circleHood.length/2];

        int largerPop = HoodToIs(circleHood,circleArrayIs,xDim/2,yDim/2);
        for (int j = 0; j < largerPop; j++) {
            Cell c = NewAgentSQ(circleArrayIs[j]);
            c.k = K0;
        }
    }

    void OriginalStep(){
        for (Cell c:this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }

    public static void runDTSims(double birthRate, double S, double gen_modifier, int sideLen, int scaleFactor, int r0) {
        String baseFilename = "_b" + Integer.toString((int) (birthRate * 100)) + "_g" + Integer.toString((int) (gen_modifier * 100)) + "_s" + Integer.toString((int) (S * 100));
        String tumorFilename = "tumor_" + baseFilename + ".csv";
        String diversityFilename = "diversity_" + baseFilename + ".csv";

        // clear file
        FileIO tumorOutputFile = new FileIO(tumorFilename, "w");
        FileIO diversityOutputFile = new FileIO(diversityFilename, "w");

        int runs = 25;
        double Ss[]=new double[runs];

        for (int i = 0; i < runs; i++) {
            String imageFilename = "final_image" + baseFilename + Integer.toString(i) + ".png";
            findDoublingTimes(birthRate, S, gen_modifier, sideLen, scaleFactor, r0, tumorOutputFile, diversityOutputFile, imageFilename, i);
            System.out.println("sim = " + i);
        }
        tumorOutputFile.Close();
        diversityOutputFile.Close();

    }

    public static void findDoublingTimes(double birthRate, double S, double gen_modifier, int sideLen, int scaleFactor, int r0, FileIO tumorOutputFile, FileIO diversityOutputFile, String imageFilename, int sim) {

        TickTimer tt=new TickTimer();
        int n0 = 1;

        // timing (tF = mod*totalSteps
        int modifier = 10;
        int totalSteps = 500;

        // make an array way too long to print out
        int tumor[] = new int[totalSteps];
        double diversity[] = new double[totalSteps];
        int jj = 0;

        // if first sim, print out time scaleX
        if (sim == 0) {
            int times[] = new int[totalSteps];
            for (int i = 0; i < totalSteps; i++) {
                times[jj] = modifier*jj;
                jj++;
            }
            jj = 0;
            tumorOutputFile.Write(ArrToString(times,",")+"\n");
            diversityOutputFile.Write(ArrToString(times,",")+"\n");
        }


        boolean run_finished = false;
        while (!run_finished) {
            //set up primary
            BirthDeath model = new BirthDeath(sideLen, r0);
            model.S = S;
            model.GENE_MUT_RATE = Math.pow(10,-gen_modifier);
            model.BIRTH_RATE = birthRate;


            int currentPopSize = n0;
            int i = 0;
            jj =0;

            while (currentPopSize < (sideLen*sideLen)*0.4) {

                tt.TickPause(0);
                model.OriginalStep();

                if (currentPopSize == 0) {
                    // this begins the while loop again
                    break;
                }

                currentPopSize = model.GetPop();
                if (i % modifier == 0) {
                    currentPopSize = model.GetPop();
                    tumor[jj] = currentPopSize;

                    // calculate diversity
                    diversity[jj] = CalculateDiversity(model, currentPopSize, model.KMAX);
                    jj++;
                }

                i++;
            }
            if (currentPopSize > 0) {
                int t = i;
                System.out.println("DT: " + (double)t*Math.log10(2) / ( Math.log10(currentPopSize) - Math.log10(n0) ));

                // exit while loop
                run_finished = true;
                currentPopSize = model.GetPop();
                tumor[jj] = currentPopSize;

                // output tumor matrix
                tumorOutputFile.Write(ArrToString(tumor,",")+"\n");
                diversityOutputFile.Write(ArrToString(diversity,",")+"\n");

                if (sim == 0) {
                    // draw the last image
                    GuiWindow win=new GuiWindow("Metastatic",false);
                    GuiGridVis Vis = new GuiGridVis(sideLen,sideLen,scaleFactor);
                    win.AddCol(0, Vis);
                    win.RunGui();
                    DrawCells(model, Vis);
                    Vis.ToPNG(imageFilename);
                    win.Dispose();
                }
            }
        }
    }

    public static void RunAndVisualize(int sideLen, int scaleFactor, int r0) {
        GuiWindow win = new GuiWindow("Metastatic", true);
        GuiGridVis Vis = new GuiGridVis(sideLen, sideLen, scaleFactor);
        win.AddCol(0, Vis);
        win.RunGui();

        TickTimer tt = new TickTimer();
        int n0;
        // timing
        int modifier = 10;

        boolean run_finished = false;
        while (!run_finished) {
            //set up primary
            BirthDeath model = new BirthDeath(sideLen, r0);

            // update params here
            model.S = 0.1;
            model.BIRTH_RATE = 0.25;
            double gen_modifier = 2.5;
            model.GENE_MUT_RATE = Math.pow(10,-gen_modifier);

            System.out.println("Min Death Rate: " + model.BIRTH_RATE*Math.pow(1 - model.S,model.KMAX));

            // make gif
            String baseFilename = "GIF_b" + Integer.toString((int) (model.BIRTH_RATE * 100)) + "_g" + Integer.toString((int) (gen_modifier * 100)) + "_s" + Integer.toString((int) (model.S * 100)) + ".gif";
            GifMaker myGif = new GifMaker(baseFilename, 100,false);

            n0 = model.GetPop();
            int currentPopSize = n0;
            int i = 0;

            while (currentPopSize < (sideLen*sideLen)*0.4) {
                DrawCells(model, Vis);
                tt.TickPause(0);
                model.OriginalStep();

                if (currentPopSize == 0) {
                    // this begins the while loop again
                    break;
                }

                currentPopSize = model.GetPop();
                if (i % modifier == 0) {
                    currentPopSize = model.GetPop();
                    myGif.AddFrame(Vis);
                }

                i++;
            }
            if (currentPopSize > 0) {
                run_finished = true;
                DrawCells(model, Vis);
                myGif.AddFrame(Vis);
                myGif.Close();
                win.Dispose();
            }


        }
    }

    public static void main(String[] args) {
        //runDTSims(double birthRate, double S, double gen_modifier, int sideLen, int scaleFactor, int r0)

//        double S = 0.5;
//
//        runDTSims(0.25, S,2.5,600,1,5);
//        runDTSims(0.25, S,3.5,600,1,5);
//
//        runDTSims(0.5, S,2.5,600,1,5);
//        runDTSims(0.5, S,3.5,600,1,5);
//
//        runDTSims(0.75, S,2.5,600,1,5);
//        runDTSims(0.75, S,3.5,600,1,5);



        // make gifs
        //RunAndVisualize(600,1, 5);






        // important parameters
        Random rn = new Random();
        BirthDeath model = new BirthDeath(100,5);
        //ClinicianSim ccl=new ClinicianSim(model,500,10,5,1,35,5,10,100, System.currentTimeMillis());
        //ccl.RunGui();
        //ccl.RunModel();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////// BirthDeath helper functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static double CalculateDiversity(BirthDeath model, int currentPopulation, int kMax) {
        int kVec[] = new int[kMax];
        for (Cell c : model) {
           kVec[c.k - 1]++;
        }

        // calculate diversity
        double sum = 0;
        for (int i = 0; i < kMax; i++) {
            if (kVec[i] > 0) {
                sum += (double)((double)kVec[i] / (double)currentPopulation)*Math.log10((double)kVec[i] / (double)currentPopulation);
            }
        }
        return Math.exp(- sum);
   }


    static void OutputPopSizes(BirthDeath t, FileIO out, int time, int kMax){
        int[]MutantCts=new int[kMax];
        Arrays.fill(MutantCts,0);

        for (Cell c : t) {
            if (c.k <= kMax) {
                MutantCts[c.k - 1]++;
            }
        }

        out.Write(ArrToString(MutantCts,",")+"\n");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////// ClinicianSim functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void DrawCells(BirthDeath model, GuiGridVis visCells) {
        for (int i = 0; i < visCells.length; i++) {
            Cell c=model.GetAgent(i);
            if(c==null){
                //background color (black)
                visCells.SetPix(i, RGB((double) 1, (double) 1, (double) 1));
            } else{
                visCells.SetPix(i, RGB((double) 1, 1.0 - ((double)c.k - 1.0)/((double)model.KMAX-1.0), (double) 0));
                visCells.SetPix(i, CategorialColor(c.k - 1));
            }
        }
    }

    @Override
    public void Draw(GuiGridVis visCells, GuiGridVis alphaVis, boolean[] switchVals) {
        for (int i = 0; i < visCells.length; i++) {
            Cell c=GetAgent(i);
            if(c==null){
                //background color (black)
                visCells.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
            } else{
                visCells.SetPix(i, CategorialColor(c.k - 1));
            }
        }
    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {
        this.BIRTH_RATE = treatmentVals[0];
        this.GENE_MUT_RATE = Math.pow(10,-(1 - treatmentVals[1])*10.0);
        this.S = treatmentVals[2];

        for (Cell c:this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }

    @Override
    public String[] GetTreatmentNames() {
        return new String[]{"Base Birth Rate", "Gene Mutation Rate", "Fitness inc, s"};
    }

    @Override
    public int[] GetTreatmentColors() {
        return new int[]{HSBColor(1.0/6,1,1),HSBColor(2.0/6,1,1),HSBColor(3.0/6,1,1)};
    }

    @Override
    public int GetNumIntensities() {
        return 30;
    }

    @Override
    public void SetupConstructors() {
        _SetupAgentListConstructor(Cell.class);
    }

    @Override
    public int VisPixX() {
        return xDim;
    }

    @Override
    public int VisPixY() {
        return yDim;
    }

    @Override
    public double GetTox() {
        return 0;
    }

    @Override
    public double GetBurden() {
        return GetPop()*1.0/length;
    }

    @Override
    public double GetMaxTox() {
        return 1.0;
    }

    @Override
    public double GetMaxBurden() {
        return 1.0;
    }

//    @Override
//    public String[] GetSwitchNames() {
//        return new String[0];
//    }
//
//    @Override
//    public boolean AllowMultiswitch() {
//        return false;
//    }
}
