package Models.Raf.Misc;
import static Framework.Utils.*;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by bravorr on 4/3/17.
 */
class NonSpatialColon {
    int maxClones;
    int[] nActiveClones;
    int nSteps;
    double mu;//mutation rate
    double[] divRates;
    double deathRate;
    int[] Ks;//carrying capacities
    double[] interactionMat;
    double[] pops;
    double[] deathPops;
    double[] swap;
    double[] typeSums;
    int[] parents;
    int nTypes;
    Random rn;

    NonSpatialColon(int maxClones,int nSteps,double mu,double deathRate,int[]Ks,double[]divRates,double[]interactionMat){
        //initing params
        if(divRates.length!=Ks.length||divRates.length*divRates.length!=interactionMat.length){ System.out.println("Error: the divRates, Ks, or interactionMatrix have improper dimensions"); }
        this.divRates=divRates;
        this.deathRate=deathRate;
        this.nTypes=this.divRates.length;
        this.Ks=Ks;
        this.interactionMat=interactionMat;
        this.maxClones=maxClones;
        this.nSteps=nSteps;
        this.mu=mu;
        this.nActiveClones=new int[this.nTypes];
        this.pops=new double[this.nTypes*this.maxClones];
        this.deathPops=new double[this.nTypes*this.maxClones];
        this.swap=new double[this.nTypes*this.maxClones];
        this.typeSums=new double[this.nTypes];
        this.rn=new Random();
        this.parents=new int[this.nTypes*this.maxClones];

        //initial conditions
        this.pops[0]=1;
        this.nActiveClones[0]=1;
        this.parents[0]=-1;
    }

    /**
     * gets the index for the pops and swapField arrays
     */
    int GetCloneI(int iType,int iClone){
        return iType*maxClones+iClone;
    }

    /**
     * sums the types for multiplication by the interaction matrix
     */
    void SumTypes(){
        Arrays.fill(typeSums,0);
        for (int iType = 0; iType < nTypes; iType++) {
            for (int iClone = 0; iClone < nActiveClones[iType] ; iClone++) {
                int iCurr=GetCloneI(iType,iClone);
                typeSums[iType]+=deathPops[iCurr];
            }
        }
    }

    /**
     * computes the interaction matrix multiplication iType row times the type sums
     */
    double ApplyInteractionMatrix(int iType){
        double ret=0;
        for (int i = 0; i < nTypes ; i++) {
            ret+=interactionMat[iType*nTypes+i]*typeSums[i];
        }
        return ret;
    }

    void ComputeDeathPops(){
        for(int iType=0;iType<nTypes;iType++){
            for (int iClone = 0; iClone < nActiveClones[iType]; iClone++) {
                int iCurr=GetCloneI(iType,iClone);
                deathPops[iCurr]=pops[iCurr]*(1-deathRate);
            }
        }
    }

    void ComputeDivPops(){
        for(int iType=0;iType<nTypes;iType++) {
            int myK = Ks[iType];
            double myDiv=divRates[iType];
            double interactionSum = ApplyInteractionMatrix(iType);
            for (int iClone = 0; iClone < nActiveClones[iType]; iClone++) {
                int iCurr=GetCloneI(iType,iClone);
                double growthRate=myDiv*((myK-interactionSum)/myK);
                swap[iCurr]=Math.exp(growthRate)*deathPops[iCurr];
            }
        }
    }

    void AddClone(int iParent,int iNewType){
        int newCloneI=GetCloneI(iNewType,nActiveClones[iNewType]);
        parents[newCloneI]=iParent;
        this.swap[newCloneI]=1;
        this.nActiveClones[iNewType]++;
    }

    void CreateNewClones(){
        for(int iType=0;iType<nTypes;iType++) {
            int myK = Ks[iType];
            for (int iClone = 0; iClone < nActiveClones[iType]; iClone++) {
                int iCurr=GetCloneI(iType,iClone);
                int nBirths=(int)(swap[iCurr]-deathPops[iCurr]);
                int nNewMuts= BinomialNaive(mu,nBirths,rn);
                swap[iCurr]-=nNewMuts;
                for (int iNewClone = 0; iNewClone < nNewMuts; iNewClone++) {
                    int iNewType=0;
                    AddClone(iCurr,iNewType);
                }
            }
        }
    }

    void Swap(){
        double[] temp=pops;
        pops=swap;
        swap=temp;
    }

    void Step(){
        ComputeDeathPops();
        SumTypes();
        ComputeDivPops();
        CreateNewClones();
        Swap();
    }

    void PrintInfo(int iStep){
        System.out.println("GetTick:"+iStep);
        ArrToString(typeSums,"typeSums");
        ArrToString(nActiveClones,"active Clones");
    }

    void Run(){
        for (int iStep = 0; iStep < nSteps ; iStep++) {
            Step();
        }
    }

    public static void main(String[] args){
        //NonSpatialColon model=new NonSpatialColon(2000,300,0.003,new int[]{1000000,1000000,1000000},new double[]{0.01,0.01,0.01},new double[]{
        //        1,1.1,1.2,
        //        0.9,1,1.1,
        //        0.8,0.9,1
        //});
        long tStart=System.currentTimeMillis();
        NonSpatialColon model=new NonSpatialColon(1000000,10000,0.003,0.01,new int[]{1000000},new double[]{1.0/7},new double[]{1});
        model.Run();
        long tDelta=System.currentTimeMillis()-tStart;
        model.PrintInfo(299);
        System.out.println("time:"+tDelta);

    }
}
