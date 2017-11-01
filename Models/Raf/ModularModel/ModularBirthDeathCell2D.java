package Models.Raf.ModularModel;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;

import java.util.Random;

import static Models.Raf.ModularModel.ModularBirthDeathModel2D.*;

public abstract class ModularBirthDeathCell2D<A extends ModularBirthDeathCell2D,G extends ModularBirthDeathModel2D<A>> extends AgentSQ2Dunstackable<G> {
    //cannot be extended or altered further
    double[]props;
    public boolean skipProbs;
    void SetupProps(){//must call first during init function
        if(!G().initialized){
            throw new IllegalStateException("can't setup cell before model is initialized");
        }
        skipProbs=false;
        if(props==null){
            props=new double[G().nProps];
        }
    }
    void Step(Random rn) {
        //get cell neighborhood
        G().nNeighborhoodIs = G().HoodToIs(G()._GetNeighborhood(Xsq(), Ysq()), G().neighborhoodIs, Xsq(), Ysq());
        G().nEmpty = 0;
        G().nOccupied = 0;
        for (int i = 0; i < G().nNeighborhoodIs; i++) {
            A cell = G().GetAgent(i);
            if (cell != null) {
                G().occupiedIs[G().nOccupied] = i;
                G().nOccupied++;
            } else {
                G().emptyIs[G().nEmpty] = i;
                G().nEmpty++;
            }
        }
        //update cell properties
        for (int i = 0; i < G().modules.size(); i++) {
            BirthDeathModule module = G().modules.get(i);
            module.StepCellProps(this);
        }
        if (this.skipProbs) {
            return;
        }
        //see if cell will die, and from what
        double dieRand = rn.nextDouble() * G().dieProbFactorSum;
        for (int i = 0; i < G().modules.size(); i++) {
            BirthDeathModule module = G().modules.get(i);
            double dieProbFactor = module.dieProbFactor;
            if (dieProbFactor != 0) {
                double probDie = module.CalcProbDie(this) * dieProbFactor;
                if (probDie > 1 || probDie < 0) {
                    throw new IllegalStateException("probability of death outside acceptible [0-1] range: " + probDie + " module index: " + i);
                }
                dieRand -= probDie;
                if (dieRand <= 0) {
                    Die(i);
                    this.skipProbs = true;
                    G()._RecordDeath(i, (A) this);
                    return;
                }
            }
        }
        //see if cell will divide, and from what
        double divRand = rn.nextDouble() * G().divProbFactorSum;
        if (G().nEmpty != 0 || G().divRule != ONLY_INTO_EMPTY_DIVIDE) {
            for (int i = 0; i < G().modules.size(); i++) {
                BirthDeathModule module = G().modules.get(i);
                double divProbFactor = module.divProbFactor;
                if (divProbFactor != 0) {
                    double probDiv = module.CalcProbDiv(this) * divProbFactor;
                    if (probDiv > 1 || probDiv < 0) {
                        throw new IllegalStateException("probability of division outside acceptible [0-1] range: " + probDiv + " module index: " + i);
                    }
                    divRand -= probDiv;
                    if (divRand <= 0) {
                        int divI = -1;
                        A prev = null;
                        //prioritize dividing into empty locations
                        if (G().nEmpty != 0 && G().divRule != RANDOM_DIVIDE) {
                            divI = G().emptyIs[rn.nextInt(G().nEmpty)];
                        }
                        //possibly divide overtop another cell, killing it
                        else {
                            divI = G().neighborhoodIs[rn.nextInt(G().nNeighborhoodIs)];
                            prev = G().GetAgent(divI);
                            if (prev != null) {
                                prev.Dispose();
                            }
                        }
                        A child = G().NewAgentSQ(divI);
                        SetupChild(i, (A) this, child);
                        G()._RecordDivide(i, (A) this, child, prev);
                        return;
                    }
                }
            }
        }
        //see if cell will move, and from what
        if (G().movementRule != NO_MOVEMENT && (G().nEmpty != 0 || G().movementRule != ONLY_INTO_EMPTY_MOVEMENT)) {
            double moveRand = rn.nextDouble() * G().moveProbFactorSum;
            for (int i = 0; i < G().modules.size(); i++) {
                BirthDeathModule module = G().modules.get(i);
                double moveProbFactor = module.moveProbFactor;
                if (moveProbFactor != 0) {
                    double probMove = module.CalcProbMove(this) * moveProbFactor;
                    if (probMove > 1 || probMove < 0) {
                        throw new IllegalStateException("probability of movement outside acceptible [0-1] range: " + probMove + " module index: " + i);
                    }
                    moveRand -= probMove;
                    if (moveRand <= 0) {
                        int iPrev = Isq();
                        //prioritize moving into emtpy locations
                        if (G().nEmpty != 0 && G().movementRule != RANDOM_MOVEMENT) {
                            int moveI = G().emptyIs[rn.nextInt(G().nEmpty)];
                            this.MoveSQ(moveI);
                            G()._RecordMove(i, iPrev, (A) this, null);
                            return;
                        }
                        //possibly swap positions with another cell
                        int moveI = G().neighborhoodIs[rn.nextInt(G().nNeighborhoodIs)];
                        A prev = G().GetAgent(moveI);
                        if (prev != null) {
                            SwapPosition(prev);
                        }
                        G()._RecordMove(i, iPrev, (A) this, prev);
                        return;
                    }
                }
            }
        }
    }

    public double GetProp(int iProp){
        return props[iProp];
    }
    public void SetProp(int iProp,double val){
        props[iProp]=val;
    }
    public PDEGrid2D GetDiff(int iDiff){
        return G().diffs[iDiff];
    }
    public int GetOccupiedI(int i){
        return G().occupiedIs[i];
    }
    public int GetEmptyI(int i){
        return G().emptyIs[i];
    }
    public int GetNeighborhoodI(int i){
        return G().neighborhoodIs[i];
    }
    public int GetLenOccupied(){
        return G().nOccupied;
    }
    public int GetLenEmpty(){
        return G().nEmpty;
    }
    public int GetLenNeighborhood(){
        return G().nNeighborhoodIs;
    }
    public abstract void Die(int reason);
    public abstract void SetupChild(int reason,A parent,A child);

}
