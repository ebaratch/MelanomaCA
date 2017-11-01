package Models.JeffreyModels.PassengerDriver;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GifMaker;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;
import Framework.Tools.FileIO;

import java.util.Random;

import static Framework.Utils.*;

class Cell extends AgentSQ2Dunstackable<PassengerDriver> {
    int kd;
    int kp;

    Cell Init(int kp0, int kd0){
        kp = kp0;
        kd = kd0;
        return this;
    }
    Cell Mutate(){
        // driver mutation
        if((G().rn.nextDouble() < ( G().Td * G().mu))) {
            kd++;
            if (kd > G().currentKMAX_driver) { G().currentKMAX_driver++; }
        }
        // passenger mutation
        if((G().rn.nextDouble() <(G().Tp * G().mu))) {
            kp++;
            if (kp > G().currentKMAX_passenger) { G().currentKMAX_passenger++; }
        }
        return this;
    }

    double GetBirthProbability(){
        //return Math.pow(1.0+G().sd,(double)kd)/Math.pow(1.0+G().sp,(double)kp)/G().Timescale;
        return 1.0/Math.pow(1.0+G().sp,(double)kp)*G().BIRTH_RATE;
    }

    double GetDeathProbability(){
        //return Math.log(1.0 + G().GetPop() / G().K)/G().Timescale/Math.pow(1.0+G().sd,(double)kd);
        //return G().GetPop() / G().K / G().Timescale / Math.pow(1.0+G().sd,(double)kd);
        return G().DEATH_RATE / Math.pow(1.0+G().sd,(double)kd);
    }



    Cell Divide(){
        int nDivOptions=G().HoodToEmptyIs(G().neighborhood,G().divIs,Xsq(),Ysq());
        if(nDivOptions==0){ return null; }
        return G().NewAgentSQ(G().divIs[G().rn.nextInt(nDivOptions)]).Init(this.kp,this.kd).Mutate();
    }
    void Step(){
        // check if birth event
        if(G().rn.nextDouble()<GetBirthProbability()){
            Divide();
        }

        // check if death event
        if(G().rn.nextDouble()<GetDeathProbability()){
            Dispose();
            return;
        }
    }
}

public class PassengerDriver extends AgentGrid2D<Cell> {

    // model params (default values)
    // mut = 10e-8 (range: 10e-10, 10e-6)
    // initial equil pop K = 10e3 (range: 10e2, 10e4)
    // target size for drivers Td ~ 700 (range 70, 70,000)
    // target size for passengers Tp ~5x10e6 (range 5x10e5, 5x10e7)
    // driver strength sd = 0.1 [ 0.01, 1]
    // passenger strength: sp = 10e-3 [10e-1, 10e-4]
    public double mu = 1e-8;
    public double sp = 1e-3;
    public double sd = 0.1;
    public double Tp = 5e6;
    public double Td = 700;
    public double K = 1e3;
    public double Timescale = 1000.0;
    public double DEATH_RATE = 0.5;
    public double BIRTH_RATE = 0.5;

    // keep track of total k's
    public int currentKMAX_passenger = 0;
    public int currentKMAX_driver = 1;

    public static int BLACK = RGB(0,0,0);

    // neighborhoods
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];
    Random rn=new Random();

    PassengerDriver(int sideLen, int r0){
        super(sideLen,sideLen, Cell.class,true,true);

        for (int x = 0; x < r0; x++) {
            for (int y = 0; y < r0; y++) {
                NewAgentSQ(x + sideLen/2 - r0 / 2,y + sideLen/2 - r0 / 2).Init(0,1);
            }
        }
    }

    void OriginalStep(){
        for (Cell c:this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }

    public static void RunAndSave(int nSims, int sideLen, int scaleFactor, int r0, int totalTime, double sp, double sd, double Tp, double Td, double mu, double birth_rate, double death_rate, int modifier) {

        String baseFilename = "CSV_sp" + Integer.toString((int) (sp * 10000)) + "_sd" + Integer.toString((int) (sd * 10000)) + "_Tp" + Integer.toString((int) (Tp )) + "_Td" + Integer.toString((int) (Td )) + "_br" + Integer.toString((int) (birth_rate* 10000)) + "_mu" + Integer.toString((int) (mu * 1000000000));
        String imageFilename = "image_" + baseFilename + ".png";
        String tumorFilename = "tumor_" + baseFilename + ".csv";
        FileIO tumorOutputFile = new FileIO(tumorFilename, "w");

        // create filenames and output files
        String passengerDiversityFilename = "passenger_diversity_" + baseFilename + ".csv";
        FileIO passengerDiversityOutputFile = new FileIO(passengerDiversityFilename,"w");
        String driverDiversityFilename = "driver_diversity_" + baseFilename + ".csv";
        FileIO driverDiversityOutputFile = new FileIO(driverDiversityFilename,"w");

        // create arrays
        int[] popArray = new int[totalTime / modifier]; // defaults to zeros i believe?
        double[] passDivArray = new double[totalTime / modifier]; // defaults to zeros i believe?
        double[] drivDivArray = new double[totalTime / modifier]; // defaults to zeros i believe?

        // add times to each as header
        for (int i = 0; i < totalTime/modifier; i++) { popArray[i] = modifier*i; }
        tumorOutputFile.Write(ArrToString(popArray,",")+"\n");
        driverDiversityOutputFile.Write(ArrToString(popArray,",")+"\n");
        passengerDiversityOutputFile.Write(ArrToString(popArray,",")+"\n");

        for (int i = 0; i < totalTime/modifier; i++) { popArray[i] = 0; }

        for (int sims = 0; sims < nSims; sims++) {
            System.out.println(sims);

            //set up primary
            PassengerDriver model = new PassengerDriver(sideLen, r0);

            model.K = r0 * r0;
            model.sp = sp;
            model.sd = sd;
            model.Tp = Tp;
            model.Td = Td;
            model.mu = mu;
            model.BIRTH_RATE = birth_rate;
            model.DEATH_RATE = death_rate;
            model.currentKMAX_passenger = 0;
            model.currentKMAX_driver = 1;

            int i = 0;
            int j = 0;
            while (i < totalTime) {
                model.OriginalStep();

                if (model.GetPop() == 0) {
                    break;
                }

                if (model.GetPop() > 1e5) {
                    break;
                }

                if (i % modifier == 0) {
                    popArray[j] = model.GetPop();
                    passDivArray[j] = GetDiversity(model, 1);
                    drivDivArray[j] = GetDiversity(model, 0);
                    j++;
                }
                i++;
            }

            for (int ii = i; ii < totalTime/modifier; ii++) { popArray[i] = 10000; }

            // after sim, print
            tumorOutputFile.Write(ArrToString(popArray,",")+"\n");
            passengerDiversityOutputFile.Write(ArrToString(passDivArray,",") + "\n");
            driverDiversityOutputFile.Write(ArrToString(drivDivArray,",")+"\n");

            // draw the last image
            if (sims == 0) {
                GuiWindow win=new GuiWindow("Metastatic",false);
                GuiGridVis Vis = new GuiGridVis(sideLen,sideLen,scaleFactor);
                win.AddCol(0, Vis);
                win.RunGui();
                DrawCells(model, Vis, 0);
                Vis.ToPNG(imageFilename);
                win.Dispose();
            }
        }
        tumorOutputFile.Close();
        passengerDiversityOutputFile.Close();
        driverDiversityOutputFile.Close();
    }


    public static void RunAndVisualize(int sideLen, int scaleFactor, int r0, int totalTime, double sp, double sd, double Tp, double Td, double mu, double birth_rate, double death_rate, int modifier) {
        GuiWindow win = new GuiWindow("Metastatic", true);
        GuiGridVis Vis1 = new GuiGridVis(sideLen, sideLen, scaleFactor);
        GuiGridVis Vis2 = new GuiGridVis(sideLen, sideLen, scaleFactor);
        win.AddCol(0, Vis1);
        win.AddCol(1,Vis2);
        win.RunGui();

        TickTimer tt = new TickTimer();
        int n0;

        boolean run_finished = false;
        while (!run_finished) {
            //set up primary
            PassengerDriver model = new PassengerDriver(sideLen, r0);

            model.K = r0*r0;
            model.sp = sp;
            model.sd = sd;
            model.Tp = Tp;
            model.Td = Td;
            model.mu = mu;
            model.BIRTH_RATE = birth_rate;
            model.DEATH_RATE = death_rate;

            // make gif
            String baseFilename = "GIF_sp" + Integer.toString((int) (sp * 10000)) + "_sd" + Integer.toString((int) (sd * 10000)) + "_Tp" + Integer.toString((int) (Tp )) + "_Td" + Integer.toString((int) (Td )) + "_mu" + Integer.toString((int) (mu * 1000000000)) + ".gif";
            GifMaker myGif = new GifMaker(baseFilename, 100,false);

            int i = 0;

            while (i < totalTime) {
                model.OriginalStep();


                if (model.GetPop() == 0) {
                    // this begins the while loop again
                    break;
                }

                // draw
                if (i % modifier == 0) {
                    //myGif.AddFrame(Vis);



                    //System.out.println("time: " + i + "  driver: " + Math.round(driv_sum*1.0/model.GetPop()) + "  pass: " + Math.round(pass_sum*1.0/model.GetPop()));
                    DrawCells(model, Vis1, 0);
                    DrawCells(model, Vis2, 1);
                    tt.TickPause(0);

                }

                if (i % 1000 == 0) {
                    int pass_sum = 0;
                    int driv_sum = 0;
                    for (Cell c : model) {
                        pass_sum+=c.kp;
                        driv_sum+=c.kd;
                    }


                    System.out.println("time: " + i + "  driver: " + Math.round(driv_sum*1.0/model.GetPop()) + "  pass: " + Math.round(pass_sum*1.0/model.GetPop()));
                }

                // update pop size
                i++;
            }
            run_finished = true;
            DrawCells(model, Vis1,0);
            DrawCells(model, Vis2,1);
            //myGif.AddFrame(Vis);
            //myGif.Close();
            win.Dispose();
        }
    }

    public static void main(String[] args) {

        // important params.
        // mut = 10e-8 (range: 10e-10, 10e-6)
        // initial equil pop K = 10e3 (range: 10e2, 10e4)
        // target size for drivers Td ~ 700 (range 70, 70,000)
        // target size for passengers Tp ~5x10e6 (range 5x10e5, 5x10e7)
        // driver strength sd = 0.1 [ 0.01, 1]
        // passenger strength: sp = 10e-3 [10e-1, 10e-4]

        // initialize w/ K cells all homogeneous
        int totalBirths = 1000;
        double mu =  1e-8;

        // initialize vecs
        double[] spvec = new double[]{0.1, 0.005, 0.0001};
        double[] sdvec = new double[]{0.01, 0.1, 1.0};
        double[] tdvec = new double[]{700};//70, 700, 7000};
        double[] tpvec = new double[]{5e5, 5e6, 5e7};
        double[] brvec = new double[]{0.1, 0.3, 0.5};

        for (int i1 = 0; i1 < spvec.length; i1++) {
            for (int i2 = 0; i2 < sdvec.length; i2++) {
                for (int i3 = 0; i3 < tdvec.length; i3++) {
                    for (int i4 = 0; i4 < tpvec.length; i4++) {
                        for (int i5 = 0; i5 < brvec.length; i5++) {
                            double sp = spvec[i1];
                            double sd = sdvec[i2];
                            double Td = tdvec[i3];
                            double Tp = tpvec[i4];
                            double birth_rate = brvec[i5]; // range 0.05 to 0.5 to not screw things up
                            int r0 = 10;
                            double death_rate = birth_rate;
                            int totalTime = (int)Math.round(totalBirths/birth_rate);
                            int modifier = totalTime / 100;

                            RunAndSave(100,600,1,r0,totalTime,sp,sd,Tp,Td,mu, birth_rate, death_rate, modifier);
                        }
                    }
                }
            }
        }


        // make gifs
        //RunAndVisualize(int sideLen, int scaleFactor, int timescale, int r0, int totalTime, double sp, double sd, double Tp, double Td, double mu)

        //RunAndVisualize(600,1,r0,totalTime,sp,sd,Tp,Td,mu, birth_rate, death_rate, modifier);
        //RunAndSave(25,600,1,r0,totalTime,sp,sd,Tp,Td,mu, birth_rate, death_rate, modifier);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////// BirthDeath helper functions
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void DrawCells(PassengerDriver model, GuiGridVis visCells, int passBool) {
        // color half by drivers and half by passengers


        for (int i = 0; i < visCells.length; i++) {
            Cell c=model.GetAgent(i);
            if(c==null){
                visCells.SetPix(i, BLACK);
            } else{
                if (passBool == 0) {
                    if (c.kd < 20) {
                        visCells.SetPix(i,CategorialColor(c.kd-1));
                    } else {
                        Random rn = new Random();
                        visCells.SetPix(i,CategorialColor(rn.nextInt(19)));
                    }

                    //visCells.SetPix(i, RGB(((double)c.kd - 1.0)/(100.0), (double) 0, (double) 0));
                } else {
                    visCells.SetPix(i, RGB(((double)c.kp - 1.0)/(100.0), (double) 0, (double) 0));
                }
            }
        }
    }

    public static double GetDiversity(PassengerDriver model, int passBool) {


        if (passBool == 0) {
            // count drivers!
            int kVec[] = new int[model.currentKMAX_driver + 1];
            for (Cell c : model) {
                kVec[c.kd - 1]++;
            }

            // calculate diversity
            double sum = 0;
            for (int i = 0; i <= model.currentKMAX_driver; i++) {
                if (kVec[i] > 0) {
                    sum += (double)((double)kVec[i] / (double)model.GetPop())*Math.log10((double)kVec[i] / (double)model.GetPop());
                }
            }
            return Math.exp(- sum);

        } else {
            // count passengers!
            int kVec[] = new int[model.currentKMAX_passenger + 1];
            for (Cell c : model) {
                kVec[c.kp]++;
            }

            // calculate diversity
            double sum = 0;
            for (int i = 0; i <= model.currentKMAX_passenger; i++) {
                if (kVec[i] > 0) {
                    sum += (double)((double)kVec[i] / (double)model.GetPop())*Math.log10((double)kVec[i] / (double)model.GetPop());
                }
            }
            return Math.exp(- sum);

        }


    }
}
