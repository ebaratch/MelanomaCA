package Models.Raf.Experimental;

/**
 * Created by rafael on 9/7/17.
 */

/*I have to create a really good transfer function
it needs to create a good distribution of values over time
while also giving power to individual neurons

sigmoid is a good one, with a volume control
there is the problem of 0-1 being non periodic, but this is probably for the best

    */
public class CellBrain {
    public double[]update;
    public double[]commands;
    public int nCommands;
    public double[]state;
    public double[][] brainHist;
    public double[] rewardHist;

    public void UpdateState(){}
    public void Learn(){

    }
}
