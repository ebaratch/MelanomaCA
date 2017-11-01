package Models.Raf.TerritoryGame;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Gui.GuiGridVis;
import Framework.Tools.GenomeInfo;
import Framework.Tools.GenomeTracker;
import Framework.Utils;

import java.awt.*;
import java.util.Random;

import static Framework.Utils.HeatMapRGB;

/**
 * Created by Rafael on 9/18/2017.
 */

class CellGenome extends GenomeInfo {
    double fitness;
    public CellGenome(GenomeInfo parent, double fitness) {
        super(parent);
        this.fitness=fitness;
    }
}

class TumorCell extends AgentSQ2Dunstackable<Tissue>{
    CellGenome genome;
    public void Init(CellGenome genome){
        this.genome=genome;
        this.genome.IncPop();
    }
    public void Die(){
        genome.DecPop();
        Dispose();
    }
    public double Mutate(){
        double newFit=G().DFE(genome.fitness);
        Tissue.CheckValidState(newFit,0,1,"DFE fitness result");
        if(newFit>=genome.fitness){
            genome=new CellGenome(genome,newFit);
        }
        return newFit;
    }
    public int Divide(int i){
        //returns number of new mutants
        TumorCell c=G().NewAgentSQ(i);
        c.Init(genome);
        int nMuts=Mutate()<0?0:1;
        nMuts=c.Mutate()<0?nMuts:nMuts+1;
        return nMuts;
    }
}

public abstract class Tissue extends AgentGrid2D<TumorCell> {
    double deathProbTumor;
    double deathProbNormal;
    double startFitness;
    public abstract double DFE(double fitness);
    public abstract int[]GetFitnessHood(int x,int y);

    static void CheckValidState(double val, double min,double max,String name){
        if(val<min||val>max){
            throw new IllegalStateException(name+" cannot go outside the range "+min+"-"+max+"!: "+val);
        }
    }
    public double[] normalFitnesses;
    int[]hood;
    int[]fitIs;
    double[] fits;
    GenomeTracker<CellGenome> tracker;
    Random rn;
    public Tissue(int xDim, int yDim,int maxHoodSize,double[]normalFitnesses,double deathProbNormal,double deathProbTumor,double startFitness,Random rn) {
        super(xDim, yDim, TumorCell.class, false, false);
        this.deathProbNormal=deathProbNormal;
        this.deathProbTumor=deathProbTumor;
        this.startFitness=startFitness;
        this.normalFitnesses=normalFitnesses;
        this.hood=new int[maxHoodSize*2];
        this.fitIs=new int[maxHoodSize];
        this.fits=new double[maxHoodSize];
        this.normalFitnesses=normalFitnesses;
        this.rn=rn;
    }
    public void DrawTissue(GuiGridVis vis){
        double min=1;
        double max=0;
        for (int i = 0; i < length; i++) {
            TumorCell c=GetAgent(i);
            if(c!=null) {
                min= Math.min(min, c.genome.fitness);
                max=Math.max(max,c.genome.fitness);
            }
        }
        for (int i = 0; i < length; i++) {
            TumorCell c=GetAgent(i);
            if(c!=null) {
                if(max!=min) {
                    vis.SetPix(i,HeatMapRGB(c.genome.fitness, min, max));
                }
                else{
                    vis.SetPix(i,HeatMapRGB(c.genome.fitness, (double) 0, (double) 1));
                }
            }
            else{

                vis.SetPix(i,Utils.RGB((double) 0, normalFitnesses[i], 1-normalFitnesses[i]));
            }
        }
        for (int j = 0; j < yDim; j++) {
            double val=j*1.0/yDim;
            vis.SetPix(0, j,Utils.RGB((double) 0, val, 1-val));
        }
        for (int j = 0; j < 3; j++) {
            vis.SetPix(j, (int)(int)Utils.Bound((int)(min*yDim)-1,0,yDim-1), Color.BLACK.getRGB());
            vis.SetPix(j, (int)Utils.Bound((int)(max*yDim)-1,0,yDim-1), Color.WHITE.getRGB());
        }
    }
    public void CheckConditions(){
        CheckValidState(deathProbNormal,0,1,"deathProbNormal");
        CheckValidState(deathProbTumor,0,1,"deathProbNormal");
        CheckValidState(startFitness,0,1,"startFitness");
        for (int i = 0; i < normalFitnesses.length; i++) {
            CheckValidState(normalFitnesses[i],0,1,"normalFitnesses grid point value");
        }
    }
    public void SetupTumor(int[] startCoords,int centerX,int centerY,int startPopSize){
        Reset();
        int[]Is=new int[startCoords.length/2];
        int nSpots=HoodToEmptyIs(startCoords,Is,centerX,centerY);
        startPopSize=nSpots<startPopSize?nSpots:startPopSize;
        CellGenome startGenome=new CellGenome(null,startFitness);
        tracker=new GenomeTracker<>(startGenome,true);
        for (int i = 0; i < startPopSize; i++) {
            TumorCell c=NewAgentSQ(Is[i]);
            c.Init(startGenome);
        }
        startGenome.DecPop();
        CheckConditions();
    }

    public CellGenome GetGenome(int i){
        TumorCell c=GetAgent(i);
        return c==null?null:c.genome;
    }
    public void Step() {
        IncTick();
        int i = rn.nextInt(length);
        int x = ItoX(i);
        int y = ItoY(i);
        TumorCell startCell = GetAgent(i);
        CellGenome startGenome = GetGenome(i);

        //drop out if no death occurs
        double n = rn.nextDouble();
        if ((startGenome == null)) {
            if (n > (deathProbNormal)) {
                return;
            } else if (deathProbNormal != 0) {
                n = n / (deathProbNormal);
            }
        } else {
            if (n > (deathProbTumor)) {
                return;
            } else if (deathProbTumor != 0) {
                n = n / (deathProbTumor);
            }
        }

        int hoodSize = HoodToIs(GetFitnessHood(x, y), fitIs, x, y);

        for (int j = 0; j < hoodSize; j++) {
            TumorCell c = GetAgent(fitIs[j]);
            fits[j] = c != null ? c.genome.fitness : normalFitnesses[fitIs[j]];
        }
        double tot=Utils.SumTo1(fits, 0, hoodSize);
        int nextPhenoI;
        if(tot==0){
            nextPhenoI = rn.nextInt(hoodSize);
        } else {
            nextPhenoI = Utils.RandomVariable(fits, n, 0, hoodSize);
        }
        TumorCell nextCell = GetAgent(fitIs[nextPhenoI]);
        if (startCell != null) {
            startCell.Die();
        }
        if (nextCell != null) {
            nextCell.Divide(i);
        }
    }


}
