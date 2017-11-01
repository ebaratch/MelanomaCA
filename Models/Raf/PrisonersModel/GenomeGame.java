package Models.Raf.PrisonersModel;
import Framework.Extensions.PayoffMatrixGame;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

import java.util.Random;

import static Framework.Utils.*;

public class GenomeGame extends PayoffMatrixGame{
    Random rn;
    TickTimer trt=new TickTimer();

    double MUTATION_PROB=0.0001;

    final static int NORMAL=0;
    final static int REPRODUCER=1;
    final static int IMMUNOSUPPRESS=2;
    final static int N_TYPES=3;

    final static int[]colors=new int[]{RGB(1,0,0), RGB(0,1,0), RGB(0,0,1)};

    final static int[]hoodEven1= HexHoodEvenY(true);
    final static int[]hoodOdd1= HexHoodOddY(true);
    final static int[]hoodEven2= HexHoodEvenY(false);
    final static int[]hoodOdd2= HexHoodOddY(false);
    final static double[] payoffMat=new double[]{
            1,1,1,
            0.95,1.1,1,
            0.9,1.5,0.9
    };
    public GenomeGame(int sideLen,Random rn){
        super(sideLen,sideLen,payoffMat,7,false,true,true);
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
            if(rn.nextDouble()<MUTATION_PROB){
                SetType(i,rn.nextInt(N_TYPES));
            }
        }
    }
    public static void main(String[] args) {
        GuiWindow win=new GuiWindow("disp",true);
        GuiGridVis vis=new GuiGridVis(400,400,1);
        win.AddCol(0, vis);
        win.RunGui();
        Random rn=new Random();
        GenomeGame g=new GenomeGame(400,rn);
        for (int i = 0; i < 100000; i++) {
            g.Step(0);
            g.DrawTypes(vis,colors);
        }
        win.Dispose();
    }
}

