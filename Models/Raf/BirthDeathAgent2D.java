package Models.Raf;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;

import java.util.Random;

/**
 * Created by Rafael on 9/5/2017.
 */

public abstract class BirthDeathAgent2D<A extends BirthDeathAgent2D,G extends AgentGrid2D<A>> extends AgentSQ2Dunstackable<G> {
    public abstract double GetDeathProb();
    public abstract double GetBirthProb();

    /**
     * @return
     * if null: the agent has died
     * if self: the agent has not died and not divided
     * if not self: the agent has divided and the return is the new agent
     */
    public A Step(int[]hood,int[]hoodIs,Random rn){
        if(rn.nextDouble()<GetDeathProb()){
            Dispose();
            return null;
        }
        if(rn.nextDouble()<GetBirthProb()) {
            int nOptions=G().HoodToEmptyIs(hood,hoodIs,Xsq(),Ysq());
            if(nOptions>0){
                return G().NewAgentSQ(hoodIs[rn.nextInt(nOptions)]);
            }
            return (A)this;
        }
        return (A)this;
    }
}
