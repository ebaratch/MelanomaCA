package Models.PhilipStuff.metabolic3DModel;

class CellConstants {
    // initialization
    // initial radius of tumor

    // cell cycle
    public double POOR_CONDITION_DEATH_RATE = 0.7f;
    public double ATP_DEATH = 0.3f;
    public double ATP_QUIESCENT = 0.8f;
    public double NORMAL_DEATH_PROB;

    // diffusion:
    // oxygen
    public double OXYGEN_MAX_RATE;
    public double OXYGEN_HALF_RATE_CONC;
    public double OXYGEN_MEDIA_CONC;
    public double OXYGEN_DIFF_RATE;
    // same for glucose
    // buffering_constat

    // metabolism
    double glycolysisPheno = 1f;
    double acidResistPheno = 6.5f;


    // movement
    double repulsionScaleFactor = 1;

    double probMigration = 0;
    public double attractionScaleFactor = 0;
    static final double MIGRATION_DISTANCE = 0.4;
    double cellRad;
    final double cellRadGrowth = 0.1;
    final double interactionRad = 2 * (cellRad + cellRadGrowth);//cell attraction
    final double divRad = cellRad * (2.0 / 3.0);
    final double maxForceDiv = 0.5;
    public double DIFF_TIME_STEP = 0.02f; //0.05f;//in seconds
    public static double CELL_TIME_STEP = 0.2f;//in days

    // artificial carrying CAPACITY
    // radius before cells start drifting back toward center
    // how much to drift toward the center

    // artificial linear gradient
    // medianRadius multiplier for beginning of gradient


}
