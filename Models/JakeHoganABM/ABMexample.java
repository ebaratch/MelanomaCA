package Models.JakeHoganABM;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by bravorr on 6/28/17.
 */

class ABMcell extends AgentSQ2Dunstackable<ABMexample> {//include that the ABMexample ("master" typeGrid class corresponds to ABMCell)
    //boolean isSurrounded; //FIXME implement this
    int rho;
    int cellCycleTime;
    int necroticTick;
    double absorbedTNFa;

    boolean IsStem(){
        return rho==-1;
    }
    boolean IsNecroticNonStem(){
        return (rho==-2);//rho==-2 is necrotic non stem cell
    }
    boolean IsNecroticStem(){
        return (rho==-3);//-3 is necrotic stem cell
    }
    void Init(int rho,boolean isStem){//use instead of constructor-used for when cell dissappears (stem cells are bigger, tells us to set whole nbhd to white?)
        //isSurrounded=false;
        cellCycleTime=0;
        if(isStem){
            this.rho=-1;
        }
        else {
            this.rho = rho;
        }
    }
    void SetNbhdColor(GuiGridVis visCells, float r,float g,float b) {
        for(int x = Xsq() - 1; x <= Xsq() + 1; x++) {
            for(int y = Ysq() - 1; y <= Ysq() + 1; y++){
                if(G().In(x, y)){
                    visCells.SetPix(x, y, RGB((double) r, (double) g, (double) b));
                }
            }
        }
    }

    void Move(GuiGridVis visCells){
        int lenToCheck=G().HoodToIs(G().mooreHood,G().localIs,Xsq(),Ysq());
        int nEmpty=G().FindEmptyIs(G().localIs,lenToCheck);//G().calls functions from corresponding Grid class
        //1st arg-the nbhd to check, 2nd arg-place to write out indices of empty nbrs, 3rd/4rth arg-x/y coord, DONT wrap around
        //return val (nEmpty) is the # of empty nbrs (int)
        int emptyI=-1;//emptyI is the index of the empty cell (starts at -1 because it hasn't been assigned)
        if(nEmpty==1){
            emptyI=0;
        }//there's only 1 elt of localIs, THAT's where cell will move (0th index of localIs is a position/indices on typeGrid)
        else if(nEmpty>1) {
            emptyI=G().rn.nextInt(nEmpty);
        }//G().rn.nextInt(nEmpty) gets a random int between 0 and nEmpty (not including nEmpty)
        if(emptyI!=-1){//if nEmpty >= 1
            if(visCells!=null){
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 1, (double) 1, (double) 1));
                if(IsStem() && !OnlyStemCellSimulation()){
                    SetNbhdColor(visCells, 1,1,1);
                }
            }//set former typeGrid point of visCells to black (reset gridpoint)
            this.MoveSQ(G().localIs[emptyI]);//move cell to location in localIs at index emptyI(emptyI came from nEmpty which was the num of empty nbrs)
        }
    }
    void Reproduce(GuiGridVis visCells){//automatically passes in visCells (a gui) by reference
        int lenToCheck=G().HoodToIs(G().mooreHood,G().localIs,Xsq(),Ysq());
        int nEmpty=G().FindEmptyIs(G().localIs,lenToCheck);
        int emptyI=-1;
        if(nEmpty==1){ emptyI=0; }
        else if(nEmpty>1) { emptyI=G().rn.nextInt(nEmpty); }
        if(emptyI!=-1) {
            //reproducing
            cellCycleTime = 0;//was age in old model- incremented every iteration
            if(!IsStem()) { rho--; }
            if (rho == 0) {//non-stem apoptosis
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 1, (double) 1, (double) 1));
                G().nNonStemCells--;
//                if(IsStem() && !OnlyStemCellSimulation()){//7/7 4pm commented out-not needed
//                    SetNbhdColor(visCells, 1,1,1);
//                }
                Dispose();//cell removed from typeGrid and in essence destroyed
                return;//getting recolored green somewhere
            }
            ABMcell daughter = G().NewAgentSQ(G().localIs[emptyI]);//creates new agent at emptyIth elt of localIs
            if (IsStem()) {
                if (G().rn.nextDouble() < G().STEM_SYMMETRIC_DIV) {
                    daughter.Init(G().MAX_RHO,true);
                    G().nStem++;
                } else {
                    daughter.Init(G().MAX_RHO,false);
                    G().nNonStemCells++;
                }
            }
            else {
                daughter.Init(rho,false);
                G().nNonStemCells++;
            }
            if (daughter.IsStem() && !OnlyStemCellSimulation()) {
                SetNbhdColor(visCells, 0, 0, 1);//make it blue
            } else if (daughter.IsStem()) {
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 0, (double) 0, (double) 1));
            }
            else {
                if(rho<=0){
                }
                visCells.SetPix(daughter.Xsq(), daughter.Ysq(), RGB((double) 0, (double) (float) ((daughter.rho * 1.0 / G().MAX_RHO) * 0.5 + 0.5), (double) 0));
            }
        }
    }

    void SetDiffusibles(){
        if (IsNecroticNonStem()||IsNecroticStem()) {
            G().tnfa.Add(Xsq()+1,Ysq()+1,G().TNFA_PRODUCTION_RATE);//Isq() index/position of cell (not i,j form)
        }else{
            double myO2 = G().o2.Get(Xsq()+1,Ysq()+1);//Isq() gets the current indices position of the cell (not i,j coordinates)
            G().o2.Set(Xsq()+1,Ysq()+1,myO2>G().O2_CONSUMPTION_RATE?myO2-G().O2_CONSUMPTION_RATE:0);//statement?if lacking o2, o2 set to 0:if false
        }
        //Added 7/13
//        double currentGridpointTNFa = G().tnfa.GetPix(Xsq(), Ysq());
//        if(absorbedTNFa + currentGridpointTNFa >= G().NECROTIC_TNFA){
//            G().tnfa.SetColor(Xsq(), Ysq(), (currentGridpointTNFa - (G().NECROTIC_TNFA - absorbedTNFa)));
//        }
    }
    boolean OnlyStemCellSimulation() {
        if(G().STEM_SYMMETRIC_DIV == 1){
            return true;
        }
        return false;
    }
    void Step(GuiGridVis visCells) {
        if (IsNecroticNonStem()|| IsNecroticStem()) {
            if(G().GetTick()-necroticTick>=G().DECOMPOSITION_TIME){//necrotic tick-time cell became necrotic
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 1, (double) 1, (double) 1));
                if(IsNecroticStem() && !OnlyStemCellSimulation()){
                        SetNbhdColor(visCells, 1,1,1);
                    }
                G().nNecroticCells--;//decrement as cells are disposed of
                Dispose();
                return;
            }
            visCells.SetPix(Xsq(), Ysq(), RGB((double) (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), (double) 0, (double) 0));
            if(IsNecroticStem() && !OnlyStemCellSimulation()){
                SetNbhdColor(visCells, (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), 0, 0);
            }
        }
        else {
            if(G().rn.nextDouble()<G().APOP_PROB){//random double 0-1
                if(!IsStem()){
                    visCells.SetPix(Xsq(), Ysq(), RGB((double) 1, (double) 1, (double) 1));
                    G().nNonStemCells--;
                }
                if(IsStem() && !OnlyStemCellSimulation()){
                    SetNbhdColor(visCells, 1, 1, 1);
                    G().nStem--;
                }
                Dispose();
                return;
            }
            //double myTNFA=G().tnfa.GetPix(Isq());
            double myO2 = G().o2.Get(Xsq()+1,Ysq()+1);//Isq() gets the current indices position of the cell (not i,j coordinates)
            if (G().tnfa.Get(Xsq()+1,Ysq()+1) > G().NECROTIC_TNFA || myO2 < G().NECROTIC_OXYGEN) {//aka if the cell becomes necrotic
                G().nNecroticCells++;
                necroticTick=G().GetTick();
                if(!IsStem()) {
                    rho = -2;//Necrotic non-stem cell
                    visCells.SetPix(Xsq(), Ysq(), RGB((double) (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), (double) 0, (double) 0));
                    G().nNonStemCells--;
                }
                if(IsStem()){
                    rho = -3;//is necrotic stem
                    G().nStem--;
                    if(!OnlyStemCellSimulation()){
                        SetNbhdColor(visCells,(float)((G().GetTick()-necroticTick)*0.7/G().DECOMPOSITION_TIME+0.3),0,0);
                    }
                }
                return;
            }
            if (myO2 < G().QUIESCENT_OXYGEN) {
                G().nHypoxic++;
                visCells.SetPix(Xsq(), Ysq(), RGB((double) (float) .5, (double) (float) .5, (double) (float) .5));
                if(IsStem() && !OnlyStemCellSimulation()) {
                    SetNbhdColor(visCells, .5f, .5f, .5f);
                }
                return;
            }
            cellCycleTime++;
            if (cellCycleTime % G().NUM_ITER_BETWEEN_MOVEMENT == 0) {//moves 48 times a day
                Move(visCells);
            }
            if (cellCycleTime >= G().ITERATIONS_PER_DAY) {
                Reproduce(visCells);//FIXME-if cell is disposed of and returns, rest of function still executed
                //ADDED 6/29 9:12
                if(!Alive()){//if cell dies because rho==0
                    return;
                }
            }
            if (IsStem() && !OnlyStemCellSimulation()) {
            SetNbhdColor(visCells, 0,0,1);//change to blue
            }else if(IsStem()){
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 0, (double) 0, (double) 1));
            } else{
                visCells.SetPix(Xsq(), Ysq(), RGB((double) 0, (double) (float) ((rho * 1.0 / G().MAX_RHO) * 0.5 + 0.5), (double) 0));
                //visCells.SetColorBound(Xsq(), Ysq(), 0, 1, 0);//FIXME gradient
            }
        }
    }
}

public class ABMexample extends AgentGrid2D<ABMcell> {//this class corresponds to ABMcell class
    //globals
    final int TIMESTEP_MS=0;
    static final int GRID_SIZE=300;//switched name from GRID_SIZE to GRID_SIZE
    static final int ITERATIONS_PER_DAY=24 * 4;//once every 6 min 96 was original val
    static final int NUM_DAYS = 200;
    static final int TIMESTEPS=ITERATIONS_PER_DAY*NUM_DAYS +1;//+1 ensures we get the last cell data written to a file
    static final int PIXEL_SIZE=2;
    static final double STEM_SYMMETRIC_DIV=1;
//    static final boolean OnlyStemCellSimulation = STEM_SYMMETRIC_DIV == 1?true:false; //replace function with this to make code more readable/editable

    //CA CONSTANTS
    final int MAX_RHO=7;
    final int CELL_WIDHT=20;
    final double NECROTIC_TNFA=0.0000385;//OK
    final double NECROTIC_OXYGEN=0.0001;
    final double QUIESCENT_OXYGEN=0.0035;//mM
    final int DAYS_BEFORE_DECOMPOSITION = 2;//?
    final int DECOMPOSITION_TIME=ITERATIONS_PER_DAY * DAYS_BEFORE_DECOMPOSITION;
    final double APOP_PROB=ProbScale(0,ITERATIONS_PER_DAY);//PROB SCALE ACCOUNTS FOR MULTIPLE ITERATIONS IN A DAY
    final int SEC_PER_DAY = 24 *3600;


    //DIFFUSIBLE CONSTANTS
//    final int DIFF_TIMESTEP= (SEC_PER_DAY)/ITERATIONS_PER_DAY;//num seconds per iteration
    final int DIFF_TIMESTEP= 1;
    final double O2_BOUNDARY_CONDITION=0.092;//mM
    final double TNFA_BOUNDARY_CONDITION=0;
    final double O2_DIFF_RATE=(1460.0/(CELL_WIDHT*CELL_WIDHT))*DIFF_TIMESTEP;//micrometers squared/sec um^2/sec. needs to be float/double
    final double TNFA_DIFF_RATE=(75.0/(CELL_WIDHT*CELL_WIDHT))*DIFF_TIMESTEP;//FIXME-time step length
    final double TNFA_PRODUCTION_RATE=0.000001*DIFF_TIMESTEP;
    final double TNFA_DEGREDATION_RATE=ProbScale((.01), SEC_PER_DAY / ITERATIONS_PER_DAY);//.00001 hz
    final double O2_CONSUMPTION_RATE=0.0005*DIFF_TIMESTEP;//mM/s
    final double O2_MAX_DIFF_THRESH=0.0001;
    final double TNFA_MAX_DIFF_THRESH=0.0001;
    //ADDED NEXT 3 LINES 6/28 6:43
    final double GRID_SQUARE_SIZE = 400;//um^2
    final double CELL_MOVEMENT_SPEED = .2;//uM/min-avgs out to every 30-60 min
    //final int CELL_MOVEMENTS_PER_DAY = (int)CELL_MOVEMENT_SPEED * 60 * 24 / 20;//* min per day/width of typeGrid (micrometers)
    final int CELL_MOVEMENTS_PER_DAY = 24;
    final int NUM_ITER_BETWEEN_MOVEMENT = ITERATIONS_PER_DAY/CELL_MOVEMENTS_PER_DAY;//cell moves every 30 min

    final TickTimer trt=new TickTimer();
    final GuiGridVis visCells;
    final GuiGridVis visO2;
    final GuiGridVis visTNFA;
    final GuiLabel cellcts;
    final int[] mooreHood=MooreHood(false);
    final int[] mooreHoodOrigin=MooreHood(true);
    final int[] localIs=new int[mooreHoodOrigin.length/2];//changed to MooreHoodOrigin 7/5

    //typeGrid components
    final PDEGrid2D o2;
    final PDEGrid2D tnfa;
    final double[] steadyCheckO2;
    final double[] steadyCheckTnfa;
    final Random rn=new Random();
    final FileIO out;//changed to final

    int nStem;
    int nNecroticCells;
    int nNonStemCells;
    int nHypoxic;
    int numAlive = nStem + nNonStemCells;

    ABMexample(int x,int y,GuiGridVis visCells,GuiGridVis visO2,GuiGridVis visTNFA,GuiLabel cellcts,String outPath){
        super(x,y,ABMcell.class);//get x,y and ABMcell.class from parent class Grid2unstackable
        o2=new PDEGrid2D(x+2,y+2);
        tnfa=new PDEGrid2D(x+2,y+2);//define 2 new grids in the "master" ABMexample class
        //initialize typeGrid
        ABMcell first=NewAgentSQ(xDim/2,yDim/2);//xDim yDim passed in as x,y I think? From parent class
        first.Init(MAX_RHO,true);//create Stem cell

//        =0;
        nNecroticCells=0;
        nStem=1;
        nNonStemCells=0;
        nHypoxic=0;

        o2.SetAll(O2_BOUNDARY_CONDITION);
        tnfa.SetAll(TNFA_BOUNDARY_CONDITION);
        this.visCells=visCells;
        this.visO2=visO2;
        this.visTNFA=visTNFA;
        this.steadyCheckO2=new double[o2.length];
        this.steadyCheckTnfa=new double[tnfa.length];
        this.cellcts=cellcts;
        if(outPath!=""){
            out=new FileIO(outPath,"w");
            out.Write("Live Cells,Stem,Non-Stem,Hypoxic,Necrotic\n");
        } else{ out=null; }
    }
    double GetMaxDiff(double[] a,double[] b){
        double max=Double.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            max=Math.max(Math.abs(a[i]-b[i]),max);
        }
        return max;
    }
    String GetModelData(int i){//changed to public
        return "Day: "+(int)((i / ITERATIONS_PER_DAY))+" Live Cells: "+numAlive+" Stem Cells: "+nStem+" Non-Stem Cells:"+nNonStemCells+" Necrotic:"+nNecroticCells+" Hypoxic:"+nHypoxic;
    }
    void WriteModelDataToFile(){
        out.Write(numAlive+","+nStem+","+nNonStemCells+","+nNecroticCells+","+nHypoxic+"\n");

    }
    boolean DiffusionStep(){
        //store current values
        System.arraycopy(o2.GetField(),0,steadyCheckO2,0,o2.length);
        System.arraycopy(tnfa.GetField(),0,steadyCheckTnfa,0,o2.length);
        for (ABMcell c : this) {
            c.SetDiffusibles();
        }
        for (int i = 0; i < tnfa.length; i++) {
            tnfa.Set(i,tnfa.Get(i) - tnfa.Get(i)*TNFA_DEGREDATION_RATE);//FIXME change to a lower rate
        }
        o2.SetOuterLayer(O2_BOUNDARY_CONDITION);
        tnfa.SetOuterLayer(TNFA_BOUNDARY_CONDITION);
        o2.DiffusionADI(O2_DIFF_RATE);
        tnfa.DiffusionADI(TNFA_DIFF_RATE);
        //compare previous values to current values
        double maxO2diff=GetMaxDiff(o2.GetField(),steadyCheckO2);
        double maxTnfadiff=GetMaxDiff(tnfa.GetField(),steadyCheckTnfa);
        if(maxO2diff<O2_MAX_DIFF_THRESH&&maxTnfadiff<TNFA_MAX_DIFF_THRESH){
            return true;
        }
        return false;
    }

    void Step(int i){
        trt.TickPause(TIMESTEP_MS);//increase TIMESTEP_MS if program is too fast
        nHypoxic=0;//get the per-step number of quiescent cells rather than a running total
        for (ABMcell c : this) {//this refers to all members of ABMcell class
            c.Step(visCells);//LOOKUP c.StepModules function
        }//set a return bool value and plug return bool val into next for loop (necrotic cells don't consume O2)
        CleanShuffInc(rn);//Clean moves unoccupied agents to end of the array, inc increments timer
        for (int j = 0; j < tnfa.length; j++) {
        }
        //for (int i = 0; i < 100; i++) {//run diffusion 100 times an iteration-FIXME shorter time steps
        int ct=0;
        while(!DiffusionStep()){
            ct++;
        }
        //}
        DrawDiffusible(visO2,o2,O2_BOUNDARY_CONDITION);//o2_boundary_condition is the MAX param
        DrawDiffusible(visTNFA,tnfa,NECROTIC_TNFA);//
//        this.cellcts.SetText(GetModelData(i));
//        if(out!=null) { WriteModelDataToFile(); }
    }
    void DrawDiffusible(GuiGridVis drawHere, PDEGrid2D drawMe, double max){//max is the max val of a certain variable eg. max.rho or max tic-necrotic.tic
        double maxVal=Double.MIN_VALUE;
        double minVal=Double.MAX_VALUE;
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                maxVal=Math.max(maxVal,drawMe.Get(x,y)/max);
                minVal=Math.min(minVal,drawMe.Get(x,y)/max);
                drawHere.SetPix(x, y, Utils.HeatMapRGB(drawMe.Get(x,y)/max));
            }
        }
    }

    public static void oneSec() {
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GuiGridVis visCells=new GuiGridVis(ABMexample.GRID_SIZE,ABMexample.GRID_SIZE,PIXEL_SIZE);//FIXME set to 2
        for(int i = 0; i < GRID_SIZE; i++){
            for(int j = 0; j < GRID_SIZE; j++){
                visCells.SetPix(i, j, RGB((double) 1, (double) 1, (double) 1));
            }
        }
        GuiGridVis visO2=new GuiGridVis(ABMexample.GRID_SIZE,ABMexample.GRID_SIZE,PIXEL_SIZE);
        GuiGridVis visTNFA=new GuiGridVis(ABMexample.GRID_SIZE,ABMexample.GRID_SIZE,PIXEL_SIZE);
        GuiLabel cellCts=new GuiLabel("|",3,1);//initialized with |, spans all 3 columns (3)?,
        ABMexample model=new ABMexample(ABMexample.GRID_SIZE,ABMexample.GRID_SIZE,visCells,visO2,visTNFA,cellCts,"outFile.csv");
        GuiWindow win=new GuiWindow("StaticModel Visualization",true);//new window
        win.AddCol(0, cellCts);
        win.AddCol(0, new GuiLabel("Stem: Blue, Normal: Green, Hypoxic: Grey, Necrotic: Red"));//adding a column automatically resizes the window-3 columns are added here
        //each col filled by col (top-> bottom)
        win.AddCol(0, visCells);
        win.AddCol(1, new GuiLabel("O2 Conc"));
        win.AddCol(1, visO2);
        win.AddCol(2, new GuiLabel("TNFA Conc"));
        win.AddCol(2, visTNFA);
        win.RunGui();
        for (int i = 0; i < ABMexample.TIMESTEPS ; i++) {
            model.Step(i);
            //ADDED 7/7
            if((i % (ITERATIONS_PER_DAY)) == 0){
            model.cellcts.SetText(model.GetModelData(i));
            if(model.out !=null) { model.WriteModelDataToFile(); }
            }
            if((i % (ITERATIONS_PER_DAY * 5)) == 0){

            }
            if((i == (ITERATIONS_PER_DAY * 5) )) {//
                int currentDay = i / ITERATIONS_PER_DAY;
                System.out.println("Day" + currentDay);
                //oneSec();
                visCells.ToGIF("Cells Day"+currentDay+".png");

            }
        }
        if(model.out!=null){ model.out.Close();}
        win.Dispose();
    }

}
