package Models.PhilipStuff.metabolic3DModel;

import Framework.Gui.GuiLabel;
import Framework.Tools.FileIO;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import static Framework.Utils.MultiThread;

public class ModelRunner {
    public static PrintStream GetOutStream(String fileName) {
        try {
            return new PrintStream(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("failed to open writer");
        }
    }

    public static double Step(int i, int runs, double min, double max) {
        double step = (max - min) / (runs - 1);
        return min + step * i;
    }

    public static void Tabulate(ArrayList<Double> toTabulate, int runs, int numVaryingParams, double min, double max) {
        for (int i = 0; i < runs / numVaryingParams; i++) {
            toTabulate.add(
                    Step(
                            i,
                            runs / numVaryingParams,
                            min,
                            max));
        }
    }

    public static void main(String[] args) {
        int numVaryingParams = 1;

        int runs = 6;
        int threads = 4;

        MarkModel3D constantHolder = new MarkModel3D(0, 0, 0, null, 0, false);

        ArrayList<Double> medianRadiusList = new ArrayList<>();
        Tabulate(medianRadiusList,
                            runs,
                            numVaryingParams,
                            0.8,
                            4);

        ArrayList<Double> maxTumorRadius = new ArrayList<>();
        Tabulate(
                maxTumorRadius,
                runs,
                numVaryingParams,
                constantHolder.maxTumorRadius - 3,
                constantHolder.maxTumorRadius + 3);

        ArrayList<Double> migrationDistances = new ArrayList<>();
        Tabulate(migrationDistances,
                runs,
                numVaryingParams,
                constantHolder.migrationDistance / 2,
                constantHolder.migrationDistance * 2);

        ArrayList<Double> atpDeathConcs = new ArrayList<>();
        Tabulate(
                atpDeathConcs,
                runs,
                numVaryingParams,
                constantHolder.atpDeathConc / 10,
                constantHolder.atpDeathConc);

        ArrayList<Double> atpQuisconc = new ArrayList<>();
        Tabulate(
                atpQuisconc,
                runs,
                numVaryingParams,
                constantHolder.atpQuiescent / 1.5,
                constantHolder.atpQuiescent * 1.5);

        ArrayList<Double> normDeathProps = new ArrayList<>();
        Tabulate(
                normDeathProps,
                runs,
                numVaryingParams,
                constantHolder.normalDeathProb / 2,
                constantHolder.normalDeathProb * 2);

        ArrayList<Double> mdaMigraionProbs = new ArrayList<>();
        Tabulate(
                mdaMigraionProbs,
                runs,
                numVaryingParams,
                constantHolder.MDAMigrationProb / 1.5,
                constantHolder.normalDeathProb * 1.5);

        ArrayList<Double> mdaGlycolysisPheos = new ArrayList<>();
        Tabulate(
                mdaGlycolysisPheos,
                runs,
                numVaryingParams,
                constantHolder.MDAGlycolysisPhenotype / 1.5,
                constantHolder.MDAGlycolysisPhenotype * 1.5);

        ArrayList<Double> mcf7Adhesions = new ArrayList<>();
        Tabulate(
                mcf7Adhesions,
                runs,
                numVaryingParams,
                constantHolder.MCF7Adhesion / 1.5,
                constantHolder.MCF7AcidResistance * 1.5);

//        ArrayList<Double>
//        Tabulate();

        ArrayList<Double> initialRatios = new ArrayList<>();
        Tabulate(initialRatios, 6, 1, 0, 1);


        /*for (double initialRatio : initialRatios) */{
            ArrayList<Metabolic3DModelResults> results=new ArrayList<>(runs);
            for (int i = 0; i < runs; i++) {
                results.add(null);
            }
            MultiThread(
                    runs,
                    threads,
                    iThread -> {
                        double initialRatio = initialRatios.get(iThread % initialRatios.size());
                        GuiLabel cellPop = new GuiLabel("cell pop: ___No started yet___");
                        MarkModel3D p = new MarkModel3D(
                                30,
                                30,
                                30,
                                cellPop,
                                30 * MarkModel3D.TIMESTEPS_PER_DAY,
                                false);

                        p.Run(
                                false,
                                markCell3DS -> {
                                    markCell3DS.Init(
                                            20000, initialRatio,
                                            markCell3DS.xDim / 2,
                                            markCell3DS.yDim / 2,
                                            markCell3DS.zDim / 2,
                                            markCell3DS.plantingRadius,
                                            markCell3DS.migrationDistance,
                                            markCell3DS.poorConditionDeathRate,
                                            markCell3DS.atpDeathConc,
                                            markCell3DS.atpQuiescent,//atpQuisconc.get(iThread % atpDeathConcs.size()),
                                            markCell3DS.normalDeathProb,
                                            markCell3DS.MDAMigrationProb,
                                            markCell3DS.MDAGlycolysisPhenotype,
                                            markCell3DS.MDAAcidResistance,
                                            markCell3DS.MCF7AcidResistance,
                                            markCell3DS.MCF7Adhesion,
                                            markCell3DS.maxTumorRadius,//maxTumorRadius.get(iThread % maxTumorRadius.size()),
                                            markCell3DS.medianRadiusGradientMultiplier,
                                            iThread);
                                });
                        //                    System.out.
                        String fileName = "3dmetab-propMDA:" + initialRatio +  ".csv";
                        FileIO out = new FileIO(fileName, "w");


                        out.Write("MCF7\tMDA\n");
                        for (int i = 0; i < p.runResults.MCF7count.length; i++) {
                            out.Write(p.runResults.MCF7count[i] + "\t" + p.runResults.MDACount[i] + "\n");
                        }



                        out.Close();
                        System.out.println("finished thread: " + iThread);
                        results.add(iThread,p.runResults);
                    }
            );
//            String fileName = "3dmetab-propMDA:" + initialRatio +  ".csv";
//            FileIO out = new FileIO(fileName, "a");
//
//            out.Write(results.get(0).params + "\n");
//
//            results.forEach(populationRatios -> {
//                out.Write("MCF7:");
//                out.WriteDelimit(populationRatios.MCF7count, ",");
//
//                out.Write("MDA:");
//                out.WriteDelimit(populationRatios.MDACount, ",");
//
//            });
//            out.Close();
        }
        System.out.println("done!");
    }

}
