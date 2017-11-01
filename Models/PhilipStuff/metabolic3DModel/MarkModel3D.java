package Models.PhilipStuff.metabolic3DModel;

import Framework.GridsAndAgents.AgentGrid3D;
import Framework.GridsAndAgents.PDEGrid3D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Gui.Vis3DOpenGL;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;


import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import static Framework.Utils.*;

/**
 * Created by bravorr on 6/15/17.
 */


//18mm diameter 1mm depth
//2x2x1
enum ExperimentalConditions {
    NORMPH_HIGHGLUC, LOWPH_HIGHGLUC,
    NORMPH_LOWGLUC, LOWPH_LOWGLUC
}

public class MarkModel3D extends AgentGrid3D<MarkCell3D> {
    final PDEGrid3D oxygen;
    final PDEGrid3D glucose;
    final PDEGrid3D acid;
    final Random rn;
    TickTimer trt;
    final double deathConcMax = 0.01;
    final double deathConcMin = 0.0001;
    final double deathConcRange = deathConcMax - deathConcMin;
    final double quiescentConc = 0.08;
    final long tickRate = 0;//100;
    final int initDiffSteps = 1000;
    final int timeSteps;
    final Vis3DOpenGL xyCellVis;
    final Vis3DOpenGL xzCellVis = null;
    final Vis3DOpenGL yzCellVis = null;

    final ExperimentalConditions experimentalConditions;
    // 0: xy. 1: yz. 2: xz
    final GuiGridVis[] cellGridVis;

    final GuiGridVis[] o2Visualizer;
    final GuiGridVis[] glucoseVisualizer;
    final GuiGridVis[] acidVisualizer;
    final GuiGridVis carryingCapacityVisualizer;
    GuiLabel cellPopulationLabel;

    public final double cellRad = 0.4;  // 0.3
    final double cellRadGrowth = 0.1;
    final float[] circlePts = GenCirclePoints(1, 20);
    final double interactionRad = 2 * (cellRad + cellRadGrowth);//cell attraction
    final double divRad = cellRad * (2.0 / 3.0);
    final double maxForceDiv = 0.5;

    public double GRID_SIZE = 20f;

    //DIFFUSION CONSTANTS
    public int CHEM_GRID_SCALE = 1;
    public double DIFF_TIME_STEP = 0.02f; //0.05f;//in seconds
    public static double CELL_TIME_STEP = 0.2f;//in days
    public static int TIMESTEPS_PER_DAY = (int) Math.round(1.0 / CELL_TIME_STEP);//in days
    //OXYGEN
    public double DELTA_SCALE = 10;
    public double OXYGEN_MAX_RATE = 0.012f * DIFF_TIME_STEP;
    public double OXYGEN_HALF_RATE_CONC = 0.005f;
    public double OXYGEN_MEDIA_CONC = 0.056f; // 0.056f
    double OXYGEN_AVG_CONC = 0.01f;
    //     double OXYGEN_AVG_CONC =OXYGEN_MEDIA_CONC;
    public double OXYGEN_DIFF_RATE = 1820f * DIFF_TIME_STEP * 1.0f / (GRID_SIZE * GRID_SIZE);
    public double OXYGEN_MAX_DELTA = 0.001f * DIFF_TIME_STEP * DELTA_SCALE;
    //GLUCOSE
    public double GLUCOSE_HALF_RATE_CONC = 0.04f;
    public double GLUCOSE_MEDIA_CONC = 4.5;
    double GLUCOSE_AVG_CONC = 2.0f;
    //     double GLUCOSE_AVG_CONC =GLUCOSE_MEDIA_CONC;
    public double GLUCOSE_DIFF_RATE = 500f * DIFF_TIME_STEP * 1.0f / (GRID_SIZE * GRID_SIZE);
    public double GLUCOSE_MAX_DELTA = 0.2f * DIFF_TIME_STEP * DELTA_SCALE;
    //ACID
    public double BUFFERING_COEFFICIENT = 0.00025f;
    public double ACID_MEDIA_CONC = PhToProtons(7.4f);
    public double ACID_DIFF_RATE = 1080f * DIFF_TIME_STEP * 1.0f / (GRID_SIZE * GRID_SIZE);
    double ACID_AVG_PROTONS = PhToProtons(7.0f);
    public double ACID_MAX_DELTA = PhToProtons(2.0f) * DIFF_TIME_STEP * DELTA_SCALE;

    //CELL CONSTANT
    public double TARGET_ATP_PRODUCTION = 29 * OXYGEN_MAX_RATE / 5; //scaled by diff timestep
    public double poorConditionDeathRate = 0.7f;
    public double atpDeathConc = 0.3f;//0.15f;//0.3f;
    public double atpQuiescent = 0.8f;

    //CELL DEATH CONSTANTS
    public double normalDeathProb = ProbScale(0.005f, CELL_TIME_STEP);


    final double[] coordScratch = new double[3];
    final float[] colorScratch = new float[3];
    final int[] colorShuff = new int[]{0, 1, 2};
    final ArrayList<MarkCell3D> cells;
    public double MDAMigrationProb = 0.3;
    public double MCF7MigrationProb = MDAMigrationProb / 8;
    public double MDAGlycolysisPhenotype = 1.1 * 1.5;
    public double MDAAcidResistance =6.4d;//6.2f;//6f; //6.2;//6.0f;
    public double MDAadhesion = 0.01;

    public double MCF7GlycolysisPhenotype = 1.1;
    public double MCF7AcidResistance = 6.5;
    public double MCF7Adhesion = cellRad / 4;


    public double maxTumorRadius = 8.33;
    public Metabolic3DModelResults runResults;
    public double medianRadiusGradientMultiplier = 4;

    GuiLabel timeLabel;

    public MarkModel3D(int x, int y, int z, GuiLabel cellPopulationLabel, int numRuns, boolean withGui) {
        super(x, y, z, MarkCell3D.class, false, false, false);
        oxygen = new PDEGrid3D(x, y, z);
        glucose = new PDEGrid3D(x, y, z);
        acid = new PDEGrid3D(x, y, z);
        rn = new Random();
        trt = new TickTimer();
        cells = new ArrayList<>();

        if (withGui) {
            timeLabel = new GuiLabel("time: 000");
            xyCellVis = new Vis3DOpenGL("xy 3D Cells", 1000, 1000, xDim, yDim, zDim, withGui);
        } else {
            xyCellVis = null;
        }

        o2Visualizer = new GuiGridVis[]{
                new GuiGridVis(oxygen.xDim, oxygen.yDim, 10, withGui),
                new GuiGridVis(oxygen.yDim, oxygen.zDim, 10, withGui),
                new GuiGridVis(oxygen.xDim, oxygen.zDim, 10, withGui)
        };
        glucoseVisualizer = new GuiGridVis[]{
                new GuiGridVis(glucose.xDim, glucose.yDim, 10, withGui),
                new GuiGridVis(glucose.yDim, glucose.zDim, 10, withGui),
                new GuiGridVis(glucose.xDim, glucose.zDim, 10, withGui)
        };
        acidVisualizer = new GuiGridVis[]{
                new GuiGridVis(acid.xDim, acid.yDim, 10, withGui),
                new GuiGridVis(acid.yDim, acid.zDim, 10, withGui),
                new GuiGridVis(acid.xDim, acid.zDim, 10, withGui)
        };
        carryingCapacityVisualizer = new GuiGridVis(1, 1, 150);
        cellGridVis = new GuiGridVis[]{
                new GuiGridVis(x, y, 10, withGui),
                new GuiGridVis(y, z, 10, withGui),
                new GuiGridVis(x, z, 10, withGui)
        };
        this.cellPopulationLabel = cellPopulationLabel;
        experimentalConditions = ExperimentalConditions.LOWPH_LOWGLUC;
        timeSteps = numRuns;
    }

    double migrationDistance = 0/*0.4*/; // initial radius / 3

    int runId = -100;
    double RandomRadius(double radius, Random random) {
        return (random.nextDouble() - 0.5) * 2 * radius;
    }

    double initialRatio = -10;
    void Init(
            int nCells,
            double proportionMDA,
            double centerX,
            double centerY,
            double centerZ,
            double plantingRadius,
            double migrationDistance,
            double poorConditionDeathRate,
            double atpDeath,
            double atpQuiescent,
            double normalDeathProb,
            double MDAMigrationProb,
            double MDAGlycolysisPhenotype,
            double MDAAcidResistance,
            double MCF7AcidResistance,
            double MCF7Adhesion,
            double maxTumorRadius,
            double medianRadiusGradientMultiplier,
            int runId) {
        this.migrationDistance = migrationDistance;
        this.poorConditionDeathRate = poorConditionDeathRate;
        this.atpDeathConc = atpDeath;
        this.atpQuiescent = atpQuiescent;
        this.normalDeathProb = normalDeathProb;

        this.MDAMigrationProb = MDAMigrationProb;
        this.MCF7MigrationProb = MDAMigrationProb / 8;

        this.MDAGlycolysisPhenotype = MDAGlycolysisPhenotype;
        this.MCF7GlycolysisPhenotype = 1 + (MDAGlycolysisPhenotype - 1) / 4;

        this.MDAAcidResistance = MDAAcidResistance;
        this.MCF7AcidResistance = MCF7AcidResistance;

        this.MCF7Adhesion = MCF7Adhesion;

        this.maxTumorRadius = maxTumorRadius;

        this.medianRadiusGradientMultiplier = medianRadiusGradientMultiplier;

        this.runId = runId;

        Init(nCells, proportionMDA, centerX, centerY, centerZ, plantingRadius);
    }
    void Init(int nCells, double proportionMDA, double centerX, double centerY, double centerZ, double radius) {
        ClearDiffusables();

        initialRatio = proportionMDA;

        int numMDA = (int) Math.ceil(nCells * proportionMDA);
        int numMCF7 = (int) Math.ceil(nCells * (1 - proportionMDA));

        for (int i = 0; i < numMDA; i++) {
            double x = RandomRadius(radius, rn) + centerX;
            double y = RandomRadius(radius, rn) + centerY;
            double z = RandomRadius(radius, rn) + centerZ;
            MarkCell3D c = NewAgentPT(x, y, z);
            c.Init(true);
        }
        for (int i = 0; i < numMCF7; i++) {
            double x = RandomRadius(radius, rn) + centerX;
            double y = RandomRadius(radius, rn) + centerY;
            double z = RandomRadius(radius, rn) + centerZ;
            MarkCell3D c = NewAgentPT(x, y, z);
            c.Init(false);
        }
    }

    double[] colorScratch_2 = new double[3];

    int GetAcidColoring(double hValue) {
        double min = 6;
        double diff = 7.4 - min;
        double phValue = (ProtonsToPh(hValue) - min) / diff;

        return HeatMapRGB(phValue);
    }

    private void DrawEnvironment() {

        o2Visualizer[0].DrawGridDiffXY(oxygen,(val1)-> HeatMapRBG(val1, (double) 0, OXYGEN_MEDIA_CONC));
        o2Visualizer[1].DrawGridDiffXZ(oxygen,(val2)-> HeatMapRGB(val2, (double) 0, OXYGEN_MEDIA_CONC));
        o2Visualizer[2].DrawGridDiffYZ(oxygen,(val1)-> HeatMapRBG(val1, (double) 0, OXYGEN_MEDIA_CONC));

        glucoseVisualizer[0].DrawGridDiffXY(glucose,(val)-> HeatMapRBG(val, (double) 0, GLUCOSE_MEDIA_CONC));
        glucoseVisualizer[1].DrawGridDiffXZ(glucose,(val1)-> HeatMapRGB(val1, (double) 0, GLUCOSE_MEDIA_CONC));
        glucoseVisualizer[2].DrawGridDiffYZ(glucose,(val)-> HeatMapRBG(val, (double) 0, GLUCOSE_MEDIA_CONC));

        acidVisualizer[0].DrawGridDiffXY(acid, this::GetAcidColoring);
        acidVisualizer[1].DrawGridDiffXZ(acid, this::GetAcidColoring);
        acidVisualizer[2].DrawGridDiffYZ(acid, this::GetAcidColoring);

//        cellGridVis[0].DrawAgentDensityXY(this, 50, "rgb");
//        cellGridVis[1].DrawAgentDensityXZ(this, 50, "rgb");
//        cellGridVis[2].DrawAgentDensityYZ(this, 50, "rgb");


//        for (int i = 0; i < 3; i++) {
////            DrawMicroEnvironment(o2Visualizer[i], oxygen::GetPix, OXYGEN_MEDIA_CONC, i);
//            DrawMicroEnvironment(glucoseVisualizer[i], glucose::GetPix, GLUCOSE_MEDIA_CONC, i);
//            DrawMicroEnvironment(cellGridVis[i], (index) -> new Integer(PopAt(index)).doubleValue(),  2 * zDim, i);
//            DrawAcid(acidVisualizer[i], i);
//        }

//        for (int x = 0; x < xDim; x++) {
//            for (int y = 0; y < yDim; y++) {
//                float red = 0;
//
//                for (int z = 0; z < zDim; z++) {
//
//                    if (GetAgent(x, y, z) != null) {
//                        red += 1d / (zDim * 2);
//                    }
//                    cellGridVis[0].SetColorHeat(x, y, red);
//                }
//            }
//        }


        cellPopulationLabel.SetText("Population: " + GetPop() + "");
        if (timeLabel != null) {
            timeLabel.SetText("time: " + GetTick());
        }
    }

    void ClearDiffusables() {
        glucose.SetAll(GLUCOSE_MEDIA_CONC);
        oxygen.SetAll(OXYGEN_MEDIA_CONC);
        acid.SetAll(ACID_MEDIA_CONC);
    }

    // returns max number of cells per pop
//    private int GetMaxCellDensity(){
//        int max = 0;
//        for (int i = 0; i < length; i++) {
//            if (PopAt(i) > max) {
//                max = PopAt(i);
//            }
//        }
//        return max;
//    }
//    private double[] GetLocationMaxCellDensity() {
//        int xOfMax = 0;
//        int yOfMax = 0;
//        int zOfMax = 0;
//        for (int x = 0; x < xDim; x++) {
//            for (int y = 0; y < yDim; y++) {
//                for (int z = 0; z < zDim; z++) {
//                    if (PopAt(x, y, z) > PopAt(xOfMax, yOfMax, zOfMax)) {
//                        xOfMax = x;
//                        yOfMax = y;
//                        zOfMax = z;
//                    }
//                }
//            }
//        }
//
//        return new double[]{xOfMax, yOfMax, zOfMax};
//    }

    void Step() {
        // system.out.println("max density: " + GetMaxCellDensity());
        trt.TickPause(tickRate);
        GetCenter(center);
        if (true/*GetMedianRadius(center[0], center[1], center[1]) > 6*/) {
            ImposeCarryingCapacity();
        }

        DiffLoop(10);

        for (MarkCell3D c : this) {
            c.Step1();
        }
        if (xyCellVis != null) {
            xyCellVis.Clear(RGB((float) 0, (float) 0, (float) 0));
        }
        for (MarkCell3D c : this) {
            c.Step2();
        }
        if (xyCellVis != null) {
            xyCellVis.Show();
        }

        DrawEnvironment();
        CleanAgents();
        IncTick();
    }

    private double GetMedianRadius(double centerX, double centerY, double centerZ) {
        ArrayList<Double> radiusList = new ArrayList<>();
        for(MarkCell3D cell : this) {
            double distanceToCenter = Norm(
                    cell.Xpt() - centerX,
                    cell.Ypt() - centerY,
                    cell.Zpt() - centerZ);
            radiusList.add(distanceToCenter);
        }

        Collections.sort(radiusList);
        return radiusList.get(radiusList.size() / 2);
    }
    public void ImposeCarryingCapacity() {
        double radiusBeforeCenterDrifting = maxTumorRadius;

        double centerPullingConstant = -0.1;
        for (MarkCell3D cell : this) {
            double dx = xDim / 2-cell.Xpt();
            double dy = yDim / 2-cell.Ypt();
            double dz = zDim / 2-cell.Zpt();

            double toCenter = Norm(dx, dy, dz);
            dx=dx/toCenter;
            dy=dy/toCenter;
            dz=dz/toCenter;

            if (toCenter > radiusBeforeCenterDrifting) {
                cell.xVel += dx *0.1* (toCenter - radiusBeforeCenterDrifting);
                cell.yVel += dy *0.1* (toCenter - radiusBeforeCenterDrifting);
                cell.zVel += dz *0.1* (toCenter - radiusBeforeCenterDrifting);
            }
        }
        carryingCapacityVisualizer.SetPix(0, Color.RED.getRGB());
    }

    public void CheckNotNegative(PDEGrid3D toCheck) {
        for (int x = 0; x < toCheck.xDim; x++) {
            for (int y = 0; y < toCheck.yDim; y++) {
                for (int z = 0; z < toCheck.zDim; z++) {
                    if (toCheck.Get(x, y, z) < 0) {
                        // system.out.println("negative value at x: " + x + " y: " + y + " z: " + z);
                    }
                }
            }
        }
    }

    double[] center = new double[3];

    void GetCenter(double ret[]) {
        ret[0] = 0;
        ret[1] = 0;
        ret[2] = 0;
        for (MarkCell3D cell : this) {
            ret[0] += cell.Xpt() / GetPop();
            ret[1] += cell.Ypt() / GetPop();
            ret[2] += cell.Zpt() / GetPop();
        }
    }

    void SetGradientDecreasingToCenter(PDEGrid3D toSet, double edgeConcentration, double centerConcentration) {
        double centerX = 0;
        double centerY = 0;
        double centerZ = 0;
        for (MarkCell3D cell : this) {
            centerX += cell.Xpt() / GetPop();
            centerY += cell.Ypt() / GetPop();
            centerZ += cell.Zpt() / GetPop();
        }

        double medianRadius = GetMedianRadius(centerX, centerY, centerZ);
        medianRadius *= medianRadiusGradientMultiplier;
//        double[] locMaxDensity = GetLocationMaxCellDensity();
//        double medianRadius = Norm(
//            locMaxDensity[0] - centerX,
//            locMaxDensity[1] - centerY,
//            locMaxDensity[2] - centerZ
//        );
        for (int x = 0; x < toSet.xDim; x++) {
            for (int y = 0; y < toSet.yDim; y++) {
                for (int z = 0; z < toSet.zDim; z++) {
                    double distanceToCenter = Norm(x - centerX, y - centerY, z - centerZ);
                    if (distanceToCenter > medianRadius) {
                        toSet.Set(x, y, z, edgeConcentration);
                    } else {
                        toSet.Set(
                                x,
                                y,
                                z,
                                (edgeConcentration - centerConcentration) * distanceToCenter / medianRadius + centerConcentration);
                    }
                }
            }
        }

    }
    final boolean linearGradientMode = false;
    final boolean densityDecreasingMode = false;

    static final int TicksBeforeGradient = 0;

    private void SetIncreasingToCenter(PDEGrid3D toSet) {
        for (int x = 0; x < toSet.xDim; x++) {
            for (int y = 0; y < toSet.yDim; y++) {
                for (int z = 0; z < toSet.zDim; z++) {
                    double currConc = toSet.GetSwap(x, y, z);
                    double currToCenter = Norm(
                            x - toSet.xDim / 2,
                            y - toSet.yDim / 2,
                            z - toSet.zDim / 2);

                    for (int xNeighbor = x - 1; xNeighbor <= x + 1; xNeighbor++) {
                        for (int yNeighbor = y - 1; yNeighbor <= y + 1; yNeighbor++) {
                            for (int zNeighbor = z - 1; zNeighbor <= z + 1; zNeighbor++) {
                                if (In(xNeighbor, yNeighbor, zNeighbor)) {
                                    double neighborToCenter = Norm(
                                            xNeighbor - toSet.xDim / 2,
                                            yNeighbor - toSet.yDim / 2,
                                            zNeighbor - toSet.zDim / 2);

                                    if (neighborToCenter <= currToCenter) {
                                        double nextConc = toSet.GetSwap(xNeighbor, yNeighbor, zNeighbor);
                                        if (nextConc > currConc) {
                                            toSet.SetSwap(x, y, z, nextConc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public void DiffLoop(int nSteps) {
        CheckNotNegative(oxygen);

        for (int i = 0; i < nSteps; i++) {
            glucose.SetAllSwap(0);
            oxygen.SetAllSwap(0);
            acid.SetAllSwap(0);
            //apply cell consumption
            for (MarkCell3D c : this) {
                c.ComputeConsumption();
            }
            if (linearGradientMode) {
                if (GetTick() > MarkModel3D.TicksBeforeGradient && GetPop() >= 180) {
                    SetGradientDecreasingToCenter(oxygen, OXYGEN_MEDIA_CONC, 0);
                    SetGradientDecreasingToCenter(glucose, GLUCOSE_MEDIA_CONC, 0);
                    SetGradientDecreasingToCenter(acid, ACID_MEDIA_CONC, PhToProtons(6.5));
                } else {
                    ClearDiffusables();
                }
            } else if (densityDecreasingMode) {
                SetIncreasingToCenter(oxygen);
                SetIncreasingToCenter(glucose);
//                acid.SetAll(PhToProtons(ACID_MEDIA_CONC));
            } else {
                glucose.Diffusion(GLUCOSE_DIFF_RATE, GLUCOSE_MEDIA_CONC, false, false, false);
                oxygen.Diffusion(OXYGEN_DIFF_RATE, OXYGEN_MEDIA_CONC, false, false, false);
                acid.Diffusion(ACID_DIFF_RATE, ACID_MEDIA_CONC, false, false, false);
            }
            for (MarkCell3D c : this) {
                c.SetConsumption();
            }
        }
    }

//    void AddCell(double x, double y, double z) {
//        MarkCell3D child = NewAgentPT(x, y, z);
//        child.Init();
//    }

    private void DrawMicroEnvironment(GuiGridVis vis, Function<Integer, Double> valueGetter, double relativeMax, int axis) {
        switch (axis) {
            // XY
            case 0:
                for (int x = 0; x < xDim; x++) {
                    for (int y = 0; y < yDim; y++) {
                        double sumZ = 0;
                        for (int z = 0; z < zDim; z++) {
                            int i = I(x, y, z);
                            sumZ += valueGetter.apply(i);
                        }

                        vis.SetPix(x, y,Utils.HeatMapRGB(sumZ / (zDim * relativeMax)));
                    }
                }
                break;
            // YZ
            case 1:
                for (int y = 0; y < yDim; y++) {
                    for (int z = 0; z < zDim; z++) {
                        double sumX = 0;
                        for (int x = 0; x < xDim; x++) {
                            int i = I(x, y, z);
                            sumX += valueGetter.apply(i);
                        }

                        vis.SetPix(y, z,Utils.HeatMapRGB(sumX / (xDim * relativeMax)));
                    }
                }
                break;
            //XZ
            case 2:
                for (int x = 0; x < xDim; x++) {
                    for (int z = 0; z < zDim; z++) {
                        double sumY = 0;
                        for (int y = 0; y < xDim; y++) {
                            int i = I(x, y, z);
                            sumY += valueGetter.apply(i);
                        }

                        vis.SetPix(x, z,Utils.HeatMapRGB(sumY / (yDim * relativeMax)));
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("axis must be 0, 1, or 2");
        }
    }

    private void DrawMicroEnvironment(GuiGridVis vis, PDEGrid3D toDraw, double relativeMax, int axis) {
        switch (axis) {
            // XY
            case 0:
                for (int x = 0; x < xDim; x++) {
                    for (int y = 0; y < yDim; y++) {
                        double sumZ = 0;
                        for (int z = 0; z < zDim; z++) {
                            sumZ += toDraw.Get(x, y, z);
                        }

                        vis.SetPix(x, y,Utils.HeatMapRGB(sumZ / (zDim * relativeMax)));
                    }
                }
                break;
            // YZ
            case 1:
                for (int y = 0; y < yDim; y++) {
                    for (int z = 0; z < zDim; z++) {
                        double sumX = 0;
                        for (int x = 0; x < xDim; x++) {
                            sumX += toDraw.Get(x, y, z);
                        }

                        try {
                            vis.SetPix(y, z,Utils.HeatMapRGB(sumX / (xDim * relativeMax)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            //XZ
            case 2:
                for (int x = 0; x < xDim; x++) {
                    for (int z = 0; z < zDim; z++) {
                        double sumY = 0;
                        for (int y = 0; y < xDim; y++) {
                            sumY += toDraw.Get(y, y, z);
                        }

                        vis.SetPix(x, z,Utils.HeatMapRGB(sumY / (yDim * relativeMax)));
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("axis must be 0, 1, or 2");
        }
    }
    private void DrawAcid(GuiGridVis vis, int axis) {
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                double sumZ = 0;
                for (int z = 0; z < acid.zDim; z++) {
                    sumZ += (Utils.ProtonsToPh(acid.Get(x, y, z)) - 6) / 1.4f;
                }

                vis.SetPix(x, y,Utils.HeatMapRGB(sumZ / zDim));
            }
        }
    }

    public void Run(boolean withGui, Consumer<MarkModel3D> customInit) {
        //dimensions of vat
        GuiWindow guiWindow = null;
        if (withGui) {
            cellPopulationLabel = new GuiLabel("cell pop: ___No started yet___");

            guiWindow = new GuiWindow("resources", false, null, withGui);

            o2Visualizer[0].Clear(RGB((double) 0.5f, (double) 0.5f, (double) 0.5f));
            glucoseVisualizer[0].Clear(RGB((double) 0.5f, (double) 0.5f, (double) 0.5f));
            acidVisualizer[0].Clear(RGB((double) 0.5f, (double) 0.5f, (double) 0.5f));

            guiWindow.AddCol(0, new GuiLabel("___"));
            guiWindow.AddCol(0, new GuiLabel("xy"));
            guiWindow.AddCol(0, new GuiLabel("yz"));
            guiWindow.AddCol(0, new GuiLabel("xz"));

            guiWindow.AddCol(1, new GuiLabel("O2 conc"));
            guiWindow.AddCol(2, new GuiLabel("glucose conc"));
            guiWindow.AddCol(3, new GuiLabel("acid conc"));
            guiWindow.AddCol(4, new GuiLabel("cell conc"));


            for (int i = 0; i < 3; i++) {
                guiWindow.AddCol(1, o2Visualizer[i]);
                guiWindow.AddCol(2, glucoseVisualizer[i]);
                guiWindow.AddCol(3, acidVisualizer[i]);
                guiWindow.AddCol(4, cellGridVis[i]);
            }

//            guiWindow.AddCol(new GuiLabel("media changing"), 5);
            guiWindow.AddCol(6, this.timeLabel);
            guiWindow.AddCol(5, carryingCapacityVisualizer);

            guiWindow.AddCol(6, cellPopulationLabel);
        }

        customInit.accept(this);
        IncTick();
        if (withGui) {
            guiWindow.RunGui();
        }

        runResults = new Metabolic3DModelResults(timeSteps);
        runResults.runId = this.runId;
        this.runResults.initialRatio = initialRatio;

//        runResults.initialRatio = init
        for (int i = 0; i < timeSteps; i++) {
//            System.out.println("i: " + i);
            Step();
            runResults.MDACount[i] = GetMDACount();
            runResults.MCF7count[i] = GetMCF7Count();
            runResults.MCF7ToMDARatio[i] =
                    ((double) runResults.MCF7count[i]) / runResults.MDACount[i];
//            GetCenter(center);
            runResults.medianRadius[i] = GetMedianRadius(center[0], center[1], center[2]);
        } //
        if(withGui){
            guiWindow.Dispose();
        }
        runResults.params = GetParamStrings(this, "atpDeathConc");
        System.out.println("done run!");
    }

    public void Run(boolean withGui) {
        Run(withGui, markCell3DS -> {
            markCell3DS.Init(10000, 0.5, xDim / 2, yDim / 2, zDim / 2, 4);
        });
    }

    public int GetMDACount() {
        int count = 0;
        for (MarkCell3D cell3D : this) {
            if (cell3D.isMDA) {
                count++;
            }
        }
        return count;
    }

    public int GetMCF7Count() {
        return GetPop() - GetMDACount();
    }


    public static String GetParamStrings(Object simObj, String... params) {
        Class<?> objClass = simObj.getClass();

        Field[] fields = objClass.getFields();
        StringBuilder ret = new StringBuilder();

        for (Field f : fields) {
            try {
                f.setAccessible(true);
                Object a = f.get(simObj);
//                System.out.println("a: " + a);

                if (Arrays.asList(params).contains(f.getName())) {
                    ret.append(f.getName()).append(": ").append(a).append(" , ");
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret.toString();
    }

    public final double plantingRadius = 4;

    public static void main(String[] args) {
        System.out.println("starting main");
        //dimensions of vat
        GuiLabel cellPop = new GuiLabel("cell pop: ___No started yet___");
        MarkModel3D p = new MarkModel3D(30, 30, 30, cellPop, 300 * MarkModel3D.TIMESTEPS_PER_DAY, true);

//        p.Run(false);
        p.Run(
                true,
                markCell3DS -> {
                    markCell3DS.Init(
                            10000, 0.5,
                            markCell3DS.xDim / 2,
                            markCell3DS.yDim / 2,
                            markCell3DS.zDim / 2,
                            markCell3DS.plantingRadius,
                            markCell3DS.migrationDistance,
                            markCell3DS.poorConditionDeathRate,
                            markCell3DS.atpDeathConc,
                            markCell3DS.atpQuiescent,
                            markCell3DS.normalDeathProb,
                            markCell3DS.MDAMigrationProb,
                            markCell3DS.MDAGlycolysisPhenotype,
                            markCell3DS.MDAAcidResistance,
                            markCell3DS.MCF7AcidResistance,
                            markCell3DS.MCF7Adhesion,
                            markCell3DS.maxTumorRadius,//StepModules(iThread, runs, markCell3DS.maxTumorRadius - 3, markCell3DS.maxTumorRadius + 3),
                            markCell3DS.medianRadiusGradientMultiplier,//StepModules(iThread, runs, 0.8, 4)
                            -10);
                });
        System.out.println("done");

        FileIO outFile = new FileIO("metab3Dpopulation.csv", "w");
        outFile.Write("MCF7, MDA\n");
        for (int i = 0; i < p.runResults.MCF7count.length; i++) {
            outFile.Write(p.runResults.MCF7count[i] + "," + p.runResults.MDACount[i] + "\n");
        }
        outFile.Close();

        System.out.println(GetParamStrings(p, "wrapX", "maxTumorRadius"));
        System.out.println("really done");
    }
}
