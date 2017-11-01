package Models.Raf.Experimental;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.Gui.GridVisWindow;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Interfaces.Brain;
import Framework.Tools.RandomNumberList;

import java.util.Random;

/**
 * Created by rafael on 9/8/17.
 */

/*

    the basic idea here is to create life that compounds on itself through evolution
    in as quick a manner as possible
    somehow both evolutionary complexity and performance need to be measured

    cells will think using a modular brain structure, which takes a state vector,
    these will use the brain interface
    */
public class LIFEGRID extends AgentGrid2D<CELL> {
    static int xDim=10;
    static int yDim=10;
    private RandomNumberList rn;

    public LIFEGRID(int x, int y, Random rn) {
        super(x, y, CELL.class,true, true);
        this.rn=new RandomNumberList(1000,10,rn);
    }

    public static void MainGui(){
        Random rn=new Random();
        GridVisWindow vis=new GridVisWindow(xDim,yDim,100);
        LIFEGRID g=new LIFEGRID(xDim,yDim,rn);
    }
    public static void main(String[] args) {
    }
}

//creatures will learn as a species
//meaning that they share experience by sharing upgrades (depending on how close they are genetically)
//there also has to be a way for them to measure how close they are genetically, that exponentially decreases
//genetics cover both intrinsic and extrinsic components of what define cells
//
class Species implements Brain {
    int brainToState;
    double[]network;
    public void UpdateBodyState(double dt) {
    }
    @Override
    public void Think(double[] state) {
    }

    @Override
    public void Learn() {
    }
}

//what is needed for an interesting world for life to develop in?
//must be vast and lush
//with ancient histories
//and magical tales
//need to add GridDiffs to make it more interesting
//randomize terrain, in such a way that multiple types of creatures must develop
//and make the combat really cool

//one griddiff should be the terrain height
//fighting from a height advantage is good
//but moving up terrain is slower
//random number generator can be accomplished by a list that is shuffled every step to save time on the random number generation.
//if the list cap is ever reached, the list size is multiplied by 10 and recalculated
class CELL extends AgentSQ2Dunstackable<LIFEGRID>{
    double[]state;
    Species species;
}
