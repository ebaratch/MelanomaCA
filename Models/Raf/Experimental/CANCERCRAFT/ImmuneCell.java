package Models.Raf.Experimental.CANCERCRAFT;

import Framework.GridsAndAgents.AgentPT2D;

import java.awt.*;

/**
 * Created by rafael on 9/14/17.
 */


//all immune cells are basically just implementations of the same thing
    //neutrophil
    //dendritic cells [miner]

    //macrophage
    //basophyl
    //eosinophyl

    //bcell(produces antibody which binds to cancer cells)
    //tcell(kills antibody bound cells)
    //Interleukin8?
    //TCells
public class ImmuneCell extends AgentPT2D<ImmuneSystem>{
    public int Bx(){
        return (int)(Xpt()*Body.SCALE);
    }
    public int By(){
        return (int)(Ypt()*Body.SCALE);
    }
    public static int DrawCell(ImmuneCell c){
        return Color.CYAN.getRGB();
    }
}
