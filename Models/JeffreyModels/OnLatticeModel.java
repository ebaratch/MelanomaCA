package Models.JeffreyModels;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GridVisWindow;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Tools.FileIO;

import java.util.Arrays;
import java.util.Random;

import static Framework.Utils.*;

class Tissue extends AgentGrid2D<Cell> {

    // cell params
    double S;
    //double DEATH_RATE;
    double BIRTH_RATE;
    double GENE_MUT_RATE;
    int K0;
    int KMAX;

    // neighborhood params
    int[]neighborhood=MooreHood(false);
    int[]divIs=new int[neighborhood.length/2];
    Random rn=new Random();

    // track population sizes 1 - 10, >10
    int[]MutantCts=new int[11];

    Tissue(int sideLen, double thisS, double thisBirthRate, double thisGeneMutRate, int thisK0, int thisKMAX){
        super(sideLen,sideLen,Cell.class,true,true);

        S = thisS;
        BIRTH_RATE = thisBirthRate;
        GENE_MUT_RATE = thisGeneMutRate;
        K0 = thisK0;
        KMAX = thisKMAX;

        // initially a single cell
        for (int xx = 0; xx <= 2; xx++) {
            for (int yy = 0; yy <= 2; yy++) {
                Cell c = NewAgentSQ(sideLen / 2 + xx, sideLen / 2 + yy);
                c.Init(K0);
            }
        }
    }
    void Step(){
        for (Cell c:this) {
            c.Step();
        }
        CleanShuffInc(rn);
    }
}

class Cell extends AgentSQ2Dunstackable<Tissue> {
    int k;

    void Init(int k0){
        k = k0;
    }
    void Mutate(){
        if((G().rn.nextDouble()<G().GENE_MUT_RATE) && (k < G().KMAX)){
            k++;
        }
    }

//    double GetBirthProbability(){
//        return G().DEATH_RATE/(Math.pow((1 - G().S),(double)k));
//    }

    double GetDeathProbability(){
        return G().BIRTH_RATE*0.99*(Math.pow((1 - G().S),(double)(k-1)));
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
        if(G().rn.nextDouble()<( G().BIRTH_RATE )){
            Divide();
        }

        if(G().rn.nextDouble()<GetDeathProbability()){
            Dispose();
            return;
        }
    }
}

public class OnLatticeModel {
    static int sideLen =500;//500;
    static int nTicks=1000000;

    // cell parameters
    static double S = 0.01;
    static double BIRTH_RATE = 0.69; // baseline death rate
    static double GENE_MUT_RATE=0.00004;//0.00004;
    static int K0 = 1;
    static int KMAX = 10;

    public static void main(String[] args) {

        //for (int KMAX = 2; KMAX <= 10; KMAX++) {
        //for (double DEATH_RATE = 0.05; KMAX <= 0.5; DEATH_RATE+=0.05) {
        //for (int ii = 2; ii <= 6; ii++) {

            //double DEATH_RATE = (double)(ii*0.05);
            System.out.println(BIRTH_RATE);


            String csvFilename = "dists_s_" + Integer.toString((int) (S * 100000)) + "_m_" + Integer.toString((int) (GENE_MUT_RATE * 100000)) + "_b" + Integer.toString((int) (BIRTH_RATE * 100000)) + "_Kmax_" + Integer.toString(KMAX) + ".csv";
            String imageFilename = "image_s_" + Integer.toString((int) (S * 100000)) + "_m_" + Integer.toString((int) (GENE_MUT_RATE * 10000)) + "_b" + Integer.toString((int) (BIRTH_RATE * 100000)) + "_Kmax_" + Integer.toString(KMAX) + ".png";

            FileIO outputFile = new FileIO(csvFilename, "w");
            outputFile.Write("1_Mutation,2_Mutations,3_Mutations,4_Mutations,5_Mutations,6_Mutations,7_Mutations,8_Mutations,9_Mutations,10_Mutations,gt10_Mutations\n");

            // multiple sims
            int nSims = 1;
            for (int sims = 0; sims < nSims; sims++) {

                System.out.println(sims);

                boolean run_finished = false;
                int currentPopSize = 9;

                while (!run_finished) {
                    Tissue t = new Tissue(sideLen, S, BIRTH_RATE, GENE_MUT_RATE, K0, KMAX);
                    for (int i = 0; i < nTicks; i++) {

                        // check popSize
                        currentPopSize = t.GetPop();


                        if ((currentPopSize == 0) || ((double) ((double) currentPopSize / (double) sideLen / (double) sideLen) > 0.01)) {//0.625)) {
                            break;
                        }

                        //win.TickPause(0);
                        t.Step();
                        //win.vis.DrawAgents(t, OnLatticeModel::CellToColor, 0, 0, 0);
                        //DrawCells(t,win);

                        if (i % 1000 == 0) {

                            System.out.println(t.GetPop()*1.0/t.length);
                            //OutputPopSizes(t, outputFile);
                        }

                    }
                    if (currentPopSize > 0) {
                        run_finished = true;
                        OutputPopSizes(t, outputFile);
                        GridVisWindow win = new GridVisWindow("OnLattice", sideLen, sideLen, 1, false);
                        Draw(t,win);
                        win.ToPNG(imageFilename);
                        //outputFile.Close();
                        win.Dispose();
                    }
                }
            }
            outputFile.Close();
            //win.Dispose();
        //}


    }
    static void Draw(Tissue t, GridVisWindow win) {
        for (int i = 0; i < win.length; i++) {
            Cell c = t.GetAgent(i);
            if(c==null){
                win.SetPix(i, RGB((double) 0, (double) 0, (double) 0));
            }
            else{
                win.SetPix(i,HSBColor((float)(c.k/11.0), (float) 1, (float) 1));
            }
        }
    }
    static int CellToColor(Cell c){
        double mutatedNess=((double)c.k-1)/10.0;
        return RGB(1,(1.0-mutatedNess),0);
    }
    static void OutputPopSizes(Tissue t, FileIO out){
        Arrays.fill(t.MutantCts,0);
        for (Cell c : t) {
            if (c.k <= 10) {
                t.MutantCts[c.k - 1]++;
            } else {
                t.MutantCts[10]++; // >10
            }
        }
        out.Write(ArrToString(t.MutantCts,",")+"\n");
    }
}
