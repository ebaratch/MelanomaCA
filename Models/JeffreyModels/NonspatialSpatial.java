package Models.JeffreyModels;

import Framework.Extensions.PayoffMatrixGame;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

import Framework.Tools.FileIO;

import java.util.Random;

import static Framework.Utils.*;

class JeffereyGameNewFormat extends PayoffMatrixGame{
    final int neighborhoodDim;
    final int[]fitnessHood;
    final int[]updateHood;
    Random rn;

    public JeffereyGameNewFormat(int x, int y,int neighborhoodDim, double[] payoffs, int maxHoodSize, boolean singleUpdate, boolean wrapX, boolean wrapY) {
        super(x, y, payoffs, maxHoodSize, singleUpdate, wrapX, wrapY);
        this.neighborhoodDim=neighborhoodDim;
        this.fitnessHood= RectangleHood(false,neighborhoodDim,neighborhoodDim);
        this.updateHood= RectangleHood(true,neighborhoodDim,neighborhoodDim);
        this.rn=new Random();
    }

    @Override
    public int[] GetFitnessHood(int x, int y) {
        return fitnessHood;
    }

    @Override
    public int[] GetReplacementHood(int x, int y) {
        return updateHood;
    }
}

public class NonspatialSpatial{
    public static void main(String[] args) {
        int sideLen=100;
        int runDur=300;
        int tickRate=100;
        double m = 1.0;
        double b = 1.35;
        double effective_b = Math.pow(b,m);
        FileIO outputFile=new FileIO("neighborhood.csv","a");

        for (int neighborhoodDim = 1; neighborhoodDim < 2; neighborhoodDim+=1) {
            double[]payoffMat=new double[]{1,0,effective_b,0};
            int[]cellCounts = new int[runDur];


            int maxSize = (int) Math.pow((2*neighborhoodDim + 1),2);

            GuiWindow win=new GuiWindow("spatial model",true);
            GuiGridVis vis=new GuiGridVis(sideLen,sideLen,5);
            win.AddCol(0, vis);
            TickTimer trt=new TickTimer();
            win.RunGui();

            JeffereyGameNewFormat g=new JeffereyGameNewFormat(100,100,neighborhoodDim,payoffMat,maxSize,false,true,true);
            g.SetupRandom(g.rn,new double[]{0.999,0.001});

            for (int i = 0; i < runDur; i++) {
                trt.TickPause(tickRate);
                g.DrawTypes(vis,new int[]{RGB(0,0,1), RGB(1,0,0)});
                g.DefaultStep(g.rn);

                int cellCount = 0;
                for (int j = 0; j < sideLen; j++) {
                    for (int k = 0; k < sideLen; k++) {
                        cellCount+=g.GetType(j,k);
                    }
                }
                cellCounts[i]=cellCount;
                System.out.println(i);

            }
            OutputCurrentGrid(cellCounts, outputFile);
//            win.Dispose();

        }
        outputFile.Close();

    }

    static void OutputCurrentGrid(int[]cellCounts, FileIO out){
        out.Write(ArrToString(cellCounts,",")+"\n");
    }
}
