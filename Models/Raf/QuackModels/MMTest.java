package Models.Raf.QuackModels;

import Models.Raf.ModularModel.BirthDeathModule;
import Models.Raf.ModularModel.ModularBirthDeathCell2D;
import Models.Raf.ModularModel.ModularBirthDeathModel2D;
import Framework.Interfaces.TreatableTumor;
import Framework.Gui.GuiGridVis;

import java.util.Random;
import static Framework.Utils.*;

class TestMudule extends BirthDeathModule<MMTest> {

    public TestMudule(MMTest myModel, double dieProbFactor, double divProbFactor, double moveProbFactor) {
        super(myModel, dieProbFactor, divProbFactor, moveProbFactor);
    }

    @Override
    public void StepDiff() {

    }

    @Override
    public void StepCellProps(ModularBirthDeathCell2D cell) {

    }

    @Override
    public double CalcProbDiv(ModularBirthDeathCell2D cell) {
        return 0.1;
    }

    @Override
    public double CalcProbDie(ModularBirthDeathCell2D cell) {
        return 0.1;
    }

    @Override
    public double CalcProbMove(ModularBirthDeathCell2D cell) {
        return 0;
    }
}

class MMCell extends ModularBirthDeathCell2D<MMCell,MMTest>{

    @Override
    public void Die(int reason) {
        Dispose();
    }

    @Override
    public void SetupChild(int reason, MMCell parent, MMCell child) {

    }
}

public class MMTest extends ModularBirthDeathModel2D<MMCell> implements TreatableTumor {
    int[]hood=MooreHood(false);
    Random rn=new Random();
    public MMTest(int x, int y, int movementRule, int divRule, boolean wrapX, boolean wrapY, int maxHoodLength) {
        super(x, y, movementRule, divRule, wrapX, wrapY, maxHoodLength, MMCell.class);
    }

    //ModularBirthDeathModel2D methods

    @Override
    public int[] _GetNeighborhood(int x, int y) {
        return hood;
    }

    @Override
    public void _RecordDeath(int reason, MMCell dead) {

    }

    @Override
    public void _RecordMove(int reason, int prevPosI, MMCell mover, MMCell swappedCell) {

    }

    @Override
    public void _RecordDivide(int reason, MMCell parent, MMCell child, MMCell killedCell) {

    }

    @Override
    public void Draw(GuiGridVis vis, GuiGridVis alphaVis, boolean[] switchVals) {
        for (int i = 0; i < vis.length; i++) {
            if(GetAgent(i)!=null) {
                vis.SetPix(i, RGB(1, 1, 1));
            }
        }
    }

    @Override
    public void QuackStep(double[] treatmentVals, int step, int stepMS) {
        this.StepModules(rn);
    }

    //TreatableTumor methods

    @Override
    public String[] GetTreatmentNames() {
        return new String[]{"Inactive"};
    }

    @Override
    public int[] GetTreatmentColors() {
        return new int[]{0};
    }

    @Override
    public int GetNumIntensities() {
        return 1;
    }

    @Override
    public void SetupConstructors() {
        this._SetupAgentListConstructor(MMCell.class);
    }

    @Override
    public int VisPixX() {
        return xDim;
    }

    @Override
    public int VisPixY() {
        return yDim;
    }

    @Override
    public double GetTox() {
        return 0;
    }

    @Override
    public double GetBurden() {
        return GetPop();
    }

    @Override
    public double GetMaxTox() {
        return 0;
    }

    @Override
    public double GetMaxBurden() {
        return length;
    }

//    @Override
//    public String[] GetSwitchNames() {
//        return new String[0];
//    }
//
//    @Override
//    public boolean AllowMultiswitch() {
//        return false;
//    }
}
