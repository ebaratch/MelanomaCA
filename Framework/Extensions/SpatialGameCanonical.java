package Framework.Extensions;

import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by rafael on 8/25/17.
 */

public abstract class SpatialGameCanonical extends SpatialGame {
    public final int[]localIs;
    public final int[]localIs2;
    public final double[]fitnessCompare;
    Random rn;
    public SpatialGameCanonical(int x, int y, int maxHoodSize, boolean wrapX, boolean wrapY){
        super(x,y,wrapX,wrapY);
        localIs=new int[maxHoodSize];
        localIs2=new int[maxHoodSize];
        fitnessCompare=new double[maxHoodSize];
        rn=new Random();
    }
    public abstract double GetFitness(int idTo,int idOther);
    public abstract void ChangeState(int idTo,int idFrom);
    public abstract int[] GetFitnessHood(int x,int y);
    public abstract int[] GetReplacementHood(int x,int y);

    public void RandomIndividualNextState(Random rn){
        int i=rn.nextInt(length);
        int nIs=SetNextStateSingle(ItoX(i), ItoY(i),rn);
            for (int j = 0; j < nIs; j++) {
                int k = localIs2[j];
                fitnesses[k] = CalcFitness(k);
            }
        }
    int SetNextStateSingle(int x,int y,Random rn){
        int nextTypeI;
        double fitnessSum=0;
        int[] hood=GetReplacementHood(x,y);
        int nIs= HoodToIs(hood,localIs2,x,y);
        for (int j = 0; j < nIs; j++) {
            double fitness=fitnesses[localIs2[j]];
            fitnessCompare[j]=fitness;
            fitnessSum+=fitness;
        }
        if(fitnessSum>0) {
            for (int j = 0; j < nIs; j++) {
                fitnessCompare[j] /= fitnessSum;
            }
            nextTypeI = RandomVariable(fitnessCompare, 0, nIs, rn);
        }
        else{
            nextTypeI=rn.nextInt(nIs);
        }
        ChangeState(I(x,y),localIs2[nextTypeI]);
        return nIs;
    }
    public void SetNextStateAll(Random rn){
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                SetNextStateSingle(x,y,rn);
            }
        }
    }
    public void StepOne(Random rn){
        RandomIndividualNextState(rn);
    }
    public void StepAll(Random rn){
        SetFitnesses();
        SetNextStateAll(rn);
    }

    @Override
    public double CalcFitness(int i) {
        int x=ItoX(i);
        int y=ItoY(i);
        double fitness = 0;
        int[] hood = GetFitnessHood(x, y);
        int nIs = HoodToIs(hood, localIs, x, y);
        for (int j = 0; j < nIs; j++) {
            fitness += GetFitness(i,localIs[j]);
        }
        return fitness/nIs;
    }

    @Override
    public double SetState(int i) {
        int x=ItoX(i);
        int y=ItoY(i);
        int nextTypeI;
        double fitnessSum=0;
        int[] hood=GetReplacementHood(x,y);
        int nIs= HoodToIs(hood,localIs2,x,y);
        for (int j = 0; j < nIs; j++) {
            double fitness=fitnesses[localIs2[j]];
            fitnessCompare[j]=fitness;
            fitnessSum+=fitness;
        }
        if(fitnessSum>0) {
            for (int j = 0; j < nIs; j++) {
                fitnessCompare[j] /= fitnessSum;
            }
            nextTypeI = RandomVariable(fitnessCompare, 0, nIs, rn);
        }
        else{
            nextTypeI=rn.nextInt(nIs);
        }
        ChangeState(i,localIs2[nextTypeI]);
        return nIs;
    }
}
