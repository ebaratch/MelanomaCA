package Models.PhilipStuff.metabolic3DModel;

public class Metabolic3DModelResults {
    // MDA / MCF7
    public double initialRatio;
    public String params;
    public int runId;
    public int[] MCF7count;
    public int[] MDACount;
    public double[] MCF7ToMDARatio;
    public double[] medianRadius;

    public Metabolic3DModelResults(int[] MCF7count, int[] MDACount, double[] MCF7ToMDARatio, double initialRatio) {
        this.MCF7count = MCF7count;
        this.MDACount = MDACount;
        this.MCF7ToMDARatio = MCF7ToMDARatio;
        this.initialRatio = initialRatio;
    }

    public Metabolic3DModelResults(int size) {
        MCF7count = new int[size];
        MDACount = new int[size];
        MCF7ToMDARatio = new double[size];
        medianRadius = new double[size];
    }
}
