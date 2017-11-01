package Models.Raf.TerritoryGame;

import Framework.Gui.GridVisWindow;
import Framework.Utils;

import java.util.Random;

/**
 * Created by Rafael on 9/19/2017.
 */
public class Example extends Tissue {
    int[]hood= Utils.VonNeumannHood(false);

    public Example(int xDim, int yDim,  Random rn,double[]normalFitnesses) {
        super(xDim, yDim, 8,normalFitnesses,0.2,0.2,0.1,new Random());
    }

    @Override
    public double DFE(double fitness) {
        double v=rn.nextDouble();
        if(v<0.00001){
            return Math.min(fitness+rn.nextDouble()*0.1,1);
        }
        return 0;
    }

    @Override
    public int[] GetFitnessHood(int x, int y) {
        return hood;
    }

    public static void main(String[] args) {
        int sideLen=200;
        double[] fitnesses=Example.GenFitnesses(sideLen,sideLen);
        int[]startCell=new int[]{0,0};
        Example ex=new Example(sideLen,sideLen,new Random(),fitnesses);
        ex.SetupTumor(startCell,ex.xDim/2,ex.yDim/2,1);
        GridVisWindow win=new GridVisWindow("TerritoryGame",sideLen,sideLen,5);
        for (int i = 0; i < 100000; i++) {
            win.TickPause(0);
            if(ex.GetPop()==0){
                ex.SetupTumor(startCell,ex.xDim/2,ex.yDim/2,1);
            }
            for (int j = 0; j < ex.length; j++) {
                ex.Step();
            }
            ex.DrawTissue(win);
        }
        win.Dispose();
    }


    static double[]GenFitnesses(int xDim,int yDim){
        double[] ret=new double[xDim*yDim];
        for (int x = 0; x <xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                ret[x*yDim+y]=NormalFitnessCalc(x,y,xDim,yDim);
            }
        }
        return ret;
    }
    public static double NormalFitnessCalc(int x, int y,double xDim,double yDim) {
        double denom=Math.sqrt((xDim/2.0)*(xDim/2.0)+(yDim/2.0)*(yDim/2.0));
        //return Math.pow(Utils.Dist(x+0.5,y+0.5,xDim/2+0.5,yDim/2+0.5)/denom,0.5);
        return Utils.Dist(x+0.5,y+0.5,xDim/2+0.5,yDim/2+0.5)/denom;
    }

}
