package Models.Raf.ModularModel2D;

/**
 * Created by Rafael on 10/17/2017.
 */

import Framework.GridsAndAgents.AgentBaseSpatial;
import Framework.GridsAndAgents.AgentGrid2D;

public class ModularModel2D  <A extends AgentBaseSpatial>extends AgentGrid2D<A> {
    public ModularModel2D(int x, int y, Class<A> agentClass, boolean wrapX, boolean wrapY) {
        super(x, y, agentClass, wrapX, wrapY);
    }
}
