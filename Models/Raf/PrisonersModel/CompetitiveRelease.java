package Models.Raf.PrisonersModel;
import Framework.Extensions.PayoffMatrixGame;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

import java.util.Random;

import static Framework.Utils.*;

public class CompetitiveRelease extends PayoffMatrixGame {
    Random rn;
    TickTimer trt=new TickTimer();

    double MUTATION_PROB=0.001;
    final static double w=1;
    final static double c=0.8;

    final static int NORMAL=0;
    final static int SENSITIVE=1;
    final static int RESISTANT=2;
    final static int N_TYPES=3;
    final static double p1 = 0.2;
    final static double p2 = 0.18;

    final static int[]colors=new int[]{RGB(0,0,1), RGB(1,0,0), RGB(0,1,0)};

    final static int[]hoodEven1= HexHoodEvenY(true);
    final static int[]hoodOdd1= HexHoodOddY(true);
    final static int[]hoodEven2= HexHoodEvenY(false);
    final static int[]hoodOdd2= HexHoodOddY(false);
    final static double[] payoffMat=new double[]{
            1 - ((1 + c)*w) + ((1 + c)*w)*1.2,1 - ((1 + c)*w) + ((1 + c)*w)*1,1 - ((1 + c)*w) + ((1 + c)*w)*1,
            1 - ((1 - c)*w) + ((1 - c)*w)*(1.2 + p1),1 - ((1 - c)*w) + ((1 - c)*w)*1.01,1 - ((1 - c)*w) + ((1 - c)*w)*1.03,
            1 - w + w*(1.2+p2),1 - w + w*(1.01 - 0.7),1 - w + w*1.02
    };
    public static double[] NewPayoffMat(double p1,double p2,double w,double c){
        return new double[]{
                1 - ((1 + c)*w) + ((1 + c)*w)*1.2,1 - ((1 + c)*w) + ((1 + c)*w)*1,1 - ((1 + c)*w) + ((1 + c)*w)*1,
                1 - ((1 - c)*w) + ((1 - c)*w)*(1.2 + p1),1 - ((1 - c)*w) + ((1 - c)*w)*1.01,1 - ((1 - c)*w) + ((1 - c)*w)*1.03,
                1 - w + w*(1.2+p2),1 - w + w*(1.01 - 0.7),1 - w + w*1.02
        };
    }
    public CompetitiveRelease(int sideLen, Random rn){
        super(sideLen,sideLen,NewPayoffMat(0.2,0.18,0.1, 0.0),7,false,true,true);
        SetupType(NORMAL);
        //SetupRandom(new double[]{0.9,0.09,0.01});
        this.rn=rn;
    }
    public void Step(long millis){
        trt.TickPause(millis);
        DefaultStep(rn);
        Mutate();
    }
    public int[] GetFitnessHood(int x, int y){
        return y%2==0?hoodEven1:hoodOdd1;
    }
    public int[] GetReplacementHood(int x, int y){
        return y%2==0?hoodEven2:hoodOdd2;
    }
    void Mutate(){
        for (int i = 0; i < length; i++) {
            int currType=GetType(i);
            if(currType!=RESISTANT&&rn.nextDouble()<MUTATION_PROB){
                SetType(i,currType+1);
            }
        }
    }
    public static void main(String[] args) {
//        double PERIOD=100;
//        double DURATION=10;

        GuiWindow win=new GuiWindow("disp",true);
        GuiGridVis vis=new GuiGridVis(100,100,10);
        win.AddCol(0, vis);
        win.RunGui();
        Random rn=new Random();
        CompetitiveRelease g=new CompetitiveRelease(100,rn);
        for (int i = 0; i < 100000; i++) {
            System.out.println(i);
            if(i>500){
                g.payoffs= NewPayoffMat(p1,p2,w,c);
            }


//            if(i%PERIOD==0){
//                g.payoffs=NewPayoffMat(p1,p2,w,0);
//            }
//            if(i%PERIOD==DURATION){
//                g.payoffs= NewPayoffMat(p1,p2,w,c);
//            }
            g.Step(10);
            g.DrawTypes(vis,colors);
        }
        win.Dispose();
    }
}

