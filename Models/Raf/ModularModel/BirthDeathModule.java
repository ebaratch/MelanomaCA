package Models.Raf.ModularModel;

public abstract class BirthDeathModule<G extends ModularBirthDeathModel2D>{
    final G myModel;
    public final double dieProbFactor;
    public final double divProbFactor;
    public final double moveProbFactor;
    public final int id;
    public BirthDeathModule(G myModel, double dieProbFactor, double divProbFactor,double moveProbFactor){
        this.myModel=myModel;
        this.dieProbFactor=dieProbFactor;
        this.divProbFactor=divProbFactor;
        this.moveProbFactor=moveProbFactor;
        if(myModel.initialized){
            throw new IllegalStateException("can't add module to initialized model");
        }
        id=myModel.modules.size();
        myModel.modules.add(this);
    }
    public int AddProp(){
        if(myModel.initialized){
            throw new IllegalStateException("can't add property to initialized model");
        }
        myModel.nProps++;
        return myModel.nProps-1;
    }
    public int AddDiff(){
        if(myModel.initialized){
            throw new IllegalStateException("can't add property to initialized model");
        }
        myModel.nDiffs++;
        return myModel.nDiffs-1;
    }
    public abstract void StepDiff();
    public abstract void StepCellProps(ModularBirthDeathCell2D cell);
    public abstract double CalcProbDiv(ModularBirthDeathCell2D cell);
    public abstract double CalcProbDie(ModularBirthDeathCell2D cell);
    public abstract double CalcProbMove(ModularBirthDeathCell2D cell);//uses VNHood order
}
