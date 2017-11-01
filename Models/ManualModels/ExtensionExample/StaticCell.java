package Models.ManualModels.ExtensionExample;

import Framework.GridsAndAgents.AgentSQ2Dunstackable;

public class StaticCell<A extends StaticCell,G extends StaticModel<A>> extends AgentSQ2Dunstackable<G> {
    public boolean isResistant;

    public void CellSetup(boolean isResistant){
        this.isResistant=isResistant;
    }
}
