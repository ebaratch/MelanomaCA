package Models.Raf.Experimental.CANCERCRAFT;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;

import java.awt.*;

/**
 * Created by rafael on 9/14/17.
 */
//body cells become cancer cells
    //this happens through mutations, these mutations unlock areas of phenotypic diversity
    //cells live and die, and mutate along this diversity it is the mutations that confer the ultimate awesome powers
    //there are other ways the cancer cells can mutate to take on their environment, however
    //3x3 axis, colormaps show their flexible phenotypic traits

    //cancer cells basically level up with mutations, allowing their phenotypes to be more flexible

    //phenotypic traits are:
// hunker down,
// multiply,
// move,
    //
// the more mutations the more they can move the traits around
    //mutations also bring antigenicity, but with luck this can be low

    //genotypic traits are: (each can be upgraded)

// cannibalize nearby normal tissue

// larger size, (fill up immune cells more)

// immortality, (later game content)

// fibro teleport, (cell swaps into fibro grid as a fibroblasts, then randomly reappears on the main grid)

// acidproducing/acid resistant

// pdl1 expressing, basically poisions immune cells

// angiogenesis, gets resource closer

// metastesize, probability of random teleport (greater near vessels?)

// other combat mutations

// probability of division

// probability of death

// probability of migration

// probability of special abilities

// buffs affect probabilities

// immune cell attacks affect these as well

// as does resource

// and internal cell state

// state variables:
    //hp
    //food
    //atp
    //

class CellGenome{
    int[] mutations;
    public static final String[] mutationNames={"CANNIBALIZE","IMMORTAL","MIGRATORY","GLYCOLYTIC","PDL1","ANGIOGENESIS","METASTASIS"};
    //mutation types
    double antigenicity;
}


// a player can set the probabilities of different mutations beforehand to customize the cancer to their playstyle
//players allocate probabilities, by clicking a point in 2D on a triangle, need to find that mapping function
//players can have up to 3 ability mutations per cell
    //have them flash different colors depending on if they have that many mutations

    //the cancer player loses when all of his mutant cells have been and killed
    //the immune player loses when the body dies

    //fibroblasts and vessels also are part of this ecosystem
    //movement happens by pushing, basically a push goes into a neighborhood nearby
    //if there is nowhere to push to, swap positions

public class BodyCell extends AgentSQ2Dunstackable<Body> {
    //TODO: ADD GENOME CLASS
    //TODO: add trifecta choice things
    double hp=0;
    double atp=0;
    double biomass=0;
    public int Cx(){
        return Xsq()/Body.SCALE;
    }
    public int Cy(){
        return Ysq()/Body.SCALE;
    }
    public void Init(int type,double hp,double atp,double biomass){
    }
    public static int DrawGenome(BodyCell b){
        return Color.WHITE.getRGB();
    }
    public static int DrawPhenotype(BodyCell b){
        return Color.GREEN.getRGB();
    }
    public void Migrate(){
        int nOpts=G().HoodToIs(G().moveHood,G().moveIs,Xsq(),Ysq());
        int nOpts2=G().FindEmptyIs(G().moveIs,G().moveOpts,nOpts);
        if(nOpts2>0){
            this.MoveSQ(G().moveOpts[G().rn.nextInt(nOpts2)]);
        }
        else{
            BodyCell c=G().GetAgent(G().rn.nextInt(nOpts));
            SwapPosition(G().GetAgent(G().rn.nextInt(nOpts)));
        }
    }
    public void Divide(){}
}
