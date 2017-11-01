package Models.Raf.ContactInhibition;

import Framework.Gui.*;
import Framework.Tools.FileIO;
import Framework.Utils;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

public class TissueMain {
    double[] bestScoreWeights;
    double bestScore;

    public TissueMain(){
        bestScore = 0.0;
        bestScoreWeights = null;
    }

    public void KeepScore(double score,double[] weights) {
        if (score > bestScore) {
            bestScoreWeights = new double[weights.length];
            System.arraycopy(weights, 0, bestScoreWeights, 0, bestScoreWeights.length);
        }
    }

    public void GenGui(GuiWindow g, ParamSet set) {
            GuiButton viewBest = new GuiButton("ViewBestGA", true,(e)->{
                g.GreyOut(true);
                GuiGridVis visActions = new GuiGridVis(g.GetInt("runSize"), g.GetInt("runSize"), g.GetInt("viewer scaleX"), 1, 1, true);
                GuiGridVis visGeno = new GuiGridVis(g.GetInt("runSize"), g.GetInt("runSize"), g.GetInt("viewer scaleX"), 1, 1, true);
                Tissue t = new Tissue(g.GetInt("runSize"), g.GetInt("runSize"), this.bestScoreWeights, g, visActions, visGeno);
                GuiWindow disp = new GuiWindow("Best Tissue", false,(f)->{
                    t.Kill();
                    g.GreyOut(false);
                }, false);
                disp.AddCol(0, visActions);
                disp.AddCol(1, visGeno);
                disp.RunGui();
                t.Run(g.GetInt("runDuration"), true, g);
                disp.Dispose();
            });
            GuiButton viewCustom =new GuiButton("View Custom", true, (e)->{
                g.GreyOut(true);
                GuiGridVis visActions = new GuiGridVis(g.GetInt("runSize"), g.GetInt("runSize"), g.GetInt("viewer scaleX"), 1, 1, true);
                GuiGridVis visGeno = new GuiGridVis(g.GetInt("runSize"), g.GetInt("runSize"), g.GetInt("viewer scaleX"), 1, 1, true);
                double[] customWts;
                switch((g.GetBool("Bias")?1:0) +(g.GetBool("Local")?1:0) + (g.GetBool("Further")?1:0)){
                    case 1: customWts = new double[]{g.GetDouble("Custom Weight1")};break;
                    case 2: customWts = new double[]{g.GetDouble("Custom Weight1"), g.GetDouble("Custom Weight2")};break;
                    case 3: customWts = new double[]{g.GetDouble("Custom Weight1"), g.GetDouble("Custom Weight2"), g.GetDouble("Custom Weight3")};break;
                    default: throw new IllegalArgumentException("Must have at least one neuron on!");
                }
                Tissue t = new Tissue(g.GetInt("runSize"), g.GetInt("runSize"), customWts, g, visActions, visGeno);
                GuiWindow disp = new GuiWindow("Best Tissue", false, (ex)->{
                    t.Kill();
                    g.GreyOut(false);
                }, false);
                disp.AddCol(0, visActions);
                disp.AddCol(1, visGeno);
                disp.RunGui();
                double[] Score = t.Run(g.GetInt("runDuration"), true, g);
                System.out.println("Start Density: "+Score[0]+" End Density: "+Score[1]+"StartPerturb: "+Score[2]+" EndPerturb: "+Score[3]+"DeathCount: "+t.deathCt);
                disp.Dispose();
            });
//            GuiButton paramSweep = new GuiButton("ParamSweep", true, (e)->{
//                g.GreyOut(true);
//                BiasNeighborSweep(g);
//                g.GreyOut(false);
//            });
            GuiButton addCommand = new GuiButton("Add Command", false, (e)->{
                FileIO out = new FileIO(g.GetString("Command File"), "a");
                if (out.length() == 0.0) {
                    out.WriteDelimit(g.LabelStrings(), ",");
                    out.Write("\n");
                }
                out.WriteDelimit(g.ValueStrings(), ",");
                out.Write("\n");
                out.Close();
            });
        GuiButton perturbSweep=new GuiButton("PertubSweep",true,(e)->{
                g.GreyOut(true);
                GuiWindow perturbOpts= new GuiWindow("PerturbSweep Options",false,(ex)->{
                   g.GreyOut(false);
                }, false);
                boolean[] running=new boolean[1];
                running[0]=true;
                perturbOpts.AddCol(0, new GuiDoubleField("point mutation probMin", 0.001, 0.0, 1.0));
                perturbOpts.AddCol(1, new GuiDoubleField("point mutation probMax", 0.1, 0.0, 1.0));
                perturbOpts.AddCol(2, new GuiLabel("Log Dist",true));
                perturbOpts.AddCol(2, new GuiBoolField("point mutation probLog", false, 1, 2));
                perturbOpts.AddCol(0, new GuiDoubleField("point mutation stdDevMin", 0.001, 0.0, 1.0));
                perturbOpts.AddCol(1, new GuiDoubleField("point mutation stdDevMax", 0.2, 0.0, 1.0));
                perturbOpts.AddCol(2, new GuiBoolField("point mutation stdDevLog", false));
                perturbOpts.AddCol(0, new GuiDoubleField("WoundRadMin", -10.0, 0.0, 50.0));
                perturbOpts.AddCol(1, new GuiDoubleField("WoundRadMax", 9.0, 0.0, 50.0));
                perturbOpts.AddCol(2, new GuiBoolField("WoundRadLog", false, 1, 2));
                perturbOpts.AddCol(0, new GuiDoubleField("WoundFreqMin", -5.0, -10.0, 50.0));
                perturbOpts.AddCol(1, new GuiDoubleField("WoundFreqMax", 9.0, 0.0, 50.0));
                perturbOpts.AddCol(2, new GuiBoolField("WoundFreqLog", false, 1, 2));
                perturbOpts.AddCol(0, new GuiDoubleField("randDeathProbMin", -10.0, 0.0, 1.0));
                perturbOpts.AddCol(1, new GuiDoubleField("randDeathProbMax", 1.0, 0.0, 1.0));
                perturbOpts.AddCol(2, new GuiBoolField("randDeathProbLog", false, 1, 3));
                perturbOpts.AddCol(0, new GuiIntField("SweepIters", 1000, 1, 10000000));
                perturbOpts.AddCol(2, new GuiButton("Start",true,(ex)->{
                    perturbOpts.GreyOut(true);
                    PerturbationSweep(g,perturbOpts,running);
                    perturbOpts.GreyOut(false);
                }));
                perturbOpts.RunGui();
            });

            GuiButton OffLatticeButton= new GuiButton("RunOffLattice",true,(e)->{
                g.GreyOut(true);
                VTOffLattice vt= new VTOffLattice(g,g.GetDouble("Custom Weight1"),g.GetDouble("Custom Weight2"),true);
                vt.Run(g.GetInt("runDuration"));
                g.GreyOut(false);
            });

            g.AddCol(0, new GuiLabel("MAIN CONTROLS"));
            g.AddCol(0, new GuiFileChooserField( "OutFile", "DefaultOut.csv"));
            g.AddCol(0, new GuiFileChooserField( "Command File", "DefaultCommands.csv"));
            g.AddCol(0, addCommand);
            g.AddCol(0, new GuiLabel("MODEL PARAMS"));
            g.AddCol(0, new GuiIntField( "runDuration", 10000, 0, 100000));
            g.AddCol(0, new GuiIntField( "runSize", 10, 0, 10000));
            g.AddCol(0, new GuiBoolField( "Bias", true));
            g.AddCol(0, new GuiBoolField( "Local", false));
            g.AddCol(0, new GuiBoolField( "Further", true));
            g.AddCol(0, new GuiBoolField( "StochasticOutput", true));
            g.AddCol(0, new GuiDoubleField( "SigmoidScale", 20.0, 0.0, 10000.0));
            g.AddCol(0, new GuiDoubleField( "BiasValue", -0.7, -10000.0, 10000.0));

            g.AddCol(1, new GuiLabel("PERTURBATIONS"));
            g.AddCol(1, new GuiIntField( "Begin Perturb", 500, 0, 1000000));
            g.AddCol(1, new GuiBoolField( "PerturbHomeostasis", false));
            g.AddCol(1, new GuiDoubleField( "randDeathProb", 0.0, 0.0, 1.0));
            g.AddCol(1, new GuiDoubleField( "WoundRad", 0.75, 0.0, 100.0));
            g.AddCol(1, new GuiIntField( "WoundFreq", 50, 0, 100));
            g.AddCol(1, new GuiDoubleField( "point mutation prob", 0.05, 0.0, 1.0));
            g.AddCol(1, new GuiDoubleField( "point mutation stdDev", 0.05, 0.0, 10.0));
            g.AddCol(1, new GuiBoolField( "RadiationMut", false));
            g.AddCol(1, new GuiBoolField( "boundWeights", true));
            g.AddCol(1, new GuiDoubleField( "weight min", 0.5, 0.0, 10.0));
            g.AddCol(1, new GuiDoubleField( "weight max", 1.0, 0.0, 10.0));

            g.AddCol(2, new GuiLabel("SWEEP PARAMS"));
            //g.AddCol(new paramSweep, 2);
            g.AddCol(2, new GuiIntField( "Sweep Runs", 20000, 0, 100000));
            g.AddCol(2, new GuiLabel("GA PARAMS"));
            //        g.AddCol(runEvo,2);
            g.AddCol(2, new GuiDoubleField( "GA mutation stdDev", 0.1, 0.0, 10.0));
            g.AddCol(2, new GuiIntField( "GA genSize", 50, 0, 1000));
            g.AddCol(2, new GuiIntField( "GA BestKeep", 5, 0, 1000));
            g.AddCol(2, new GuiIntField( "GA numGens", 6, 0, 100));

            g.AddCol(3, new GuiLabel("VIEWER CONTROLS"));
            g.AddCol(3, new GuiIntField( "viewer scaleX", 5, 1, 100));
            g.AddCol(3, new GuiDoubleField( "viewer timestep", 0.0, 0.0, 10.0));
            g.AddCol(2, perturbSweep);
            g.AddCol(3, viewBest);
            g.AddCol(3, viewCustom);
            g.AddCol(3, new GuiDoubleField( "Custom Weight1", 0.2, -100.0, 100.0));
            g.AddCol(3, new GuiDoubleField( "Custom Weight2", 1.0, -100.0, 100.0));
            g.AddCol(3, new GuiDoubleField( "Custom Weight3", 0.5, -100.0, 100.0));
            g.AddCol(3, new GuiLabel("RESULTS CONTROLS"));
            g.AddCol(3, new GuiDoubleField( "MinStartDensity", 0.5, 0.0, 1.0));
            g.AddCol(3, new GuiDoubleField( "MaxStartDensity", 0.9, 0.0, 1.0));
//            g.AddCol(GuiDoubleField(g, "divHeuristicValue", -0.5, -10.0, 10.0), 3)
//            g.AddCol(GuiDoubleField(set, "waitHeuristicValue", 1.0, 0.0, 10.0), 3)
//            g.AddCol(GuiDoubleField(set, "waitHeuristicExp", 10.0, 0.0, 20.0), 3)
            g.AddCol(3, new GuiBoolField( "RecMutations", false));
            g.AddCol(3, new GuiFileChooserField( "MutationsOutFile", "DefaultMut.csv"));
            g.AddCol(4, OffLatticeButton);
            g.AddCol(4, new GuiDoubleField("cellRad",0.2,0.01,1));
            g.AddCol(4, new GuiDoubleField("divRad",2.0/3.0,0,1));
            g.AddCol(4, new GuiDoubleField("friction",0.5,0,1));
            g.AddCol(4, new GuiDoubleField("forceExp",2,0,10));
            g.AddCol(4, new GuiDoubleField("forceMul",1,0,10));
            g.AddCol(4, new GuiDoubleField("thinkForceScale",100,0,10000));
            g.AddCol(4, new GuiComboBoxField("VisColor",0,new String[]{"MutationColor","ForceColor","BothColor"}));
        }

        void PerturbationSweep(GuiWindow set, GuiWindow perturbSet, boolean[] running){
            String[] varStrings={"point mutation prob","point mutation stdDev","WoundRad","WoundFreq","randDeathProb"};
            ParamSet sweepSet=new ParamSet(set.LabelStrings(), set.ValueStrings());
            int nRuns=perturbSet.GetInt("SweepIters");
            ArrayList<String> sweepOut=new ArrayList<>(nRuns);
            for (int i = 0; i < nRuns; i++) {
                sweepOut.add(null);
            }
            MultiThread(nRuns,4,(iThread)->{
                if(running[0]) {
                    Random rn =new Random();
                    ParamSet runSet = new ParamSet(sweepSet.LabelStrings(), sweepSet.ValueStrings());
                    double[] sweepParams = new double[5];
                    for (int i=0;i<varStrings.length;i++) {
                        String s = varStrings[i];
                        double min = perturbSet.GetDouble(s+"Min");
                        double max = perturbSet.GetDouble(s+"GetMax");
                        double sweepParam = perturbSet.GetBool(s+"Log")?Math.max(LogDist(min, max, rn),0.0):Math.max(rn.nextDouble() * (max - min) + min,0.0);
                        sweepParams[i] = sweepParam;
                        runSet.Set(s, Double.toString(sweepParam));
                    }
                    Tissue t = new Tissue(runSet.GetInt("runSize"), runSet.GetInt("runSize"), new double[]{runSet.GetDouble("Custom Weight1"), runSet.GetDouble("Custom Weight2")}, set, null, null);

                    double[] out = t.Run(runSet.GetInt("runDuration"), false, set);
                    double[] mutrec=t.MutRecordAverage();
                    sweepOut.set(iThread,ArrToString(sweepParams, ",") + ArrToString(out, ",") + Utils.ArrToString(t.progenitorWeights,",") + ArrToString(mutrec,","));
                }
                else { sweepOut.set(iThread,"");}
            });
            FileIO out=new FileIO(set.GetString("OutFile"),"w");
            out.Write(ArrToString(varStrings,",")+"StartDensity,EndDensity,StartTick,EndTick,w1i,w2i,w1f,w2f,\n");
            out.WriteStrings(sweepOut,"\n");
            out.Close();
            System.out.println("wrote results");
        }

        public void BiasNeighborSweep(GuiWindow set) {
            int nNeurons = (set.GetBool("Bias")?1:0) +(set.GetBool("Local")?1:0) + (set.GetBool("Further")?1:0);
            String weightNames = "";
            Random rn = new Random();
            int nRuns=set.GetInt("Sweep Runs");
            ArrayList<String> sweepOut=new ArrayList<>(nRuns);
            MultiThread(nRuns,4,(iThread)->{
                double[] params = new double[nNeurons];
                double weightMin = set.GetDouble("weight min");
                double weightMax = set.GetDouble("weight max");
                for (int i=0;i<nNeurons;i++) {
                    params[i] = rn.nextDouble() * (weightMax - weightMin) + weightMin;
                }
                Tissue t = new Tissue(set.GetInt("runSize"), set.GetInt("runSize"), params, set, null, null);
                double[] res = t.Run(set.GetInt("runDuration"), false, set);
                String ret="";
                ret= ArrToString(params,",");
                double[] avgs = t.MutRecordAverage();
                ret+= ArrToString(res,",");
                ret+=t.deathCt+","+avgs[0]+","+avgs[1]+"\n";
                sweepOut.set(iThread, ret);
            });
            FileIO out = new FileIO(set.GetString("OutFile"), "w");
            if (set.GetBool("Bias")) {
                out.Write("Bias,");
            }
            if (set.GetBool("Local")) {
                out.Write("Local,");
            }
            if (set.GetBool("Further")) {
                out.Write("Further,");
            }
            out.Write(weightNames + "StartDensity,EndDensity,StartTick,EndTick,DeathCount,w1Avg,w2Avg\n");
            for (String s : sweepOut) {
                out.Write(s);
            }
            out.Close();
        }

        void RunCommandSet(String commandFilePath, GuiWindow set) {
            FileIO commandFile = new FileIO(commandFilePath, "r");
            ArrayList<String[]> commands = commandFile.ReadDelimit(",");
            set.SetLables(commands.get(0));
            int nCommands = commands.size();
            for (int i=1;i<=nCommands;i++) {
                set.SetParamValues(commands.get(i));
                BiasNeighborSweep(set);
                System.out.println("Commpleted command " + i + " out of " + (nCommands - 1));
            }
        }

    public static void main(String[] args) {
        ParamSet set = new ParamSet();
        TissueMain tm = new TissueMain();
        GuiWindow g = new GuiWindow("Virtual Tissue II", true);
        tm.GenGui(g, set);
        g.RunGui();
        //tm.RunCommandSet("PosterData/Commands.csv",set);
    }
}
