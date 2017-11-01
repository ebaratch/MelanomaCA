 /*
default idea64.exe.vmoptions //i increased memory to 2 gigs
//go to help Edit Custom VM options to change back
# custom IntelliJ IDEA VM options
-Xms128m
-Xmx750m
rest is the same
 */
package Models.JakeHoganABM;

import Framework.GridsAndAgents.*;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;
import Framework.Utils;

import java.util.Arrays;
import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by Jake Hogan on 7/13/2017.
 */

class ABMcell3d2 extends AgentSQ3Dunstackable<ABM3D2> {
    int rho;
    int cellCycleTime;
    int necroticTick;
    boolean isHypoxic;
    boolean isSurrounded;

    boolean IsStem(){
        return (rho==-1||rho==-4);//-1 stem cell not only stem cell simulation, -4 stem cell, stem cell only simulation
    }
    boolean IsNecroticNonStem(){
        return (rho==-2);//rho==-2 is necrotic non stem cell
    }
    boolean IsNecroticStem(){
        return (rho==-3);//-3 is necrotic stem cell
    }
    void Init(int rho,boolean isStem){//use instead of constructor-used for when cell dissappears (stem cells are bigger, tells us to set whole nbhd to white?)
//        isSurrounded=false;
        cellCycleTime=0;
        if(isStem && !OnlyStemCellSimulation()){
            this.rho=-1;
        }
        else if(isStem && OnlyStemCellSimulation()){
            this.rho=-4;
        }
        else {
            this.rho = rho;
        }
    }

    public int FindEmptyNeighbors3d(int[] SQs, int[]ret, int centerX, int centerY, int centerZ, boolean wrapX, boolean wrapY, boolean wrapZ){
        int nIs=G().HoodToIs(SQs,ret,centerX,centerY,centerZ, wrapX,wrapY, wrapZ);//ret means return
        int validCount=0;
        for (int i = 0; i < nIs; i++) {
            if(G().GetAgent(ret[i])==null){
                ret[validCount]=ret[i];
                validCount++;
            }
        }
        return validCount;
    }
//    void SetNbhdColor(GuiGridVis visCells, float r,float g,float b) { //        for(int x = Xsq() - 1; x <= Xsq() + 1; x++) {
//            for(int y = Ysq() - 1; y <= Ysq() + 1; y++){
//              for(int z = Zsq() - 1; z <= Zsq() + 1; y++) {
//                  if (G().In(x, y, z)) {
//                      visCells.SetColor(x, y, r, g, b);
//                  }
//              }
//            }
//        }
//    }

    void Move(){
        int nEmpty= FindEmptyNeighbors3d(G().mooreHood,G().localIs,Xsq(),Ysq(),Zsq(),false, false, false);//G().calls functions from corresponding Grid class
        //1st arg-the nbhd to check, 2nd arg-place to write out indices of empty nbrs, 3rd/4rth arg-x/y coord, DONT wrap around
        //return val (nEmpty) is the # of empty nbrs (int)
        if(nEmpty == 0){
            isSurrounded = true;
            return;
        }
        else{
            isSurrounded = false;
        }
        int emptyI=-1;//emptyI is the index of the empty cell (starts at -1 because it hasn't been assigned)
        if(nEmpty==1){
            emptyI=0;
        }//there's only 1 elt of localIs, THAT's where cell will move (0th index of localIs is a position/indices on typeGrid)
        else if(nEmpty>1) {
            emptyI=G().rn.nextInt(nEmpty);
        }//G().rn.nextInt(nEmpty) gets a random int between 0 and nEmpty (not including nEmpty)
        if(emptyI!=-1) {//if nEmpty >= 1
//            if(visCells!=null){
//                visCells.SetColor(Xsq(),Ysq(),1,1,1);
//                if(IsStem() && !OnlyStemCellSimulation()){
//                    SetNbhdColor(visCells, 1,1,1);
//                }
//              }
            //set former typeGrid point of visCells to black (reset gridpoint)
            int moveX = G().ItoX(G().localIs[emptyI]);//FIXME-recheck if broken 7/14
            int moveY = G().ItoY(G().localIs[emptyI]);
            int moveZ = G().ItoZ(G().localIs[emptyI]);
            MoveSQ(moveX, moveY, moveZ);//move cell to location in localIs at index emptyI(emptyI came from nEmpty which was the num of empty nbrs)
        }
    }
    void Reproduce(){//automatically passes in visCells (a gui) by reference
        int nEmpty= FindEmptyNeighbors3d(G().mooreHood,G().localIs,Xsq(),Ysq(),Zsq(),false,false,false);
        if(nEmpty == 0){
            isSurrounded = true;
            return;
        }
        else{
            isSurrounded = false;
        }
        int emptyI=-1;
        if(nEmpty==1){ emptyI=0; }
        else if(nEmpty>1) { emptyI=G().rn.nextInt(nEmpty); }
        if(emptyI!=-1) {
            //reproducing
            cellCycleTime = 0;//was age in old model- incremented every iteration
            if(!IsStem()) { rho--; }
            if (rho == 0) {//non-stem apoptosis
//                visCells.SetColor(Xsq(),Ysq(),1,1,1);//make dead cells disappear from typeGrid
                G().nNonStemCells--;
//                if(IsStem() && !OnlyStemCellSimulation()){//7/7 4pm commented out-not needed
//                    SetNbhdColor(visCells, 1,1,1);
//                }
                Dispose();//cell removed from typeGrid and in essence destroyed
                return;//getting recolored green somewhere
            }
            ABMcell3d2 daughter = G().NewAgentSQ(G().localIs[emptyI]);//creates new agent at emptyIth elt of localIs
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
//            if (daughter.IsStem() && !OnlyStemCellSimulation()) {
//                SetNbhdColor(visCells, 0, 0, 1);//make it blue
//            } else if (daughter.IsStem()) {
//                visCells.SetColor(Xsq(), Ysq(), 0, 0, 1);//make it blue
//            }
//            else {
//                if(rho<=0){
//                }
//                visCells.SetColor(daughter.Xsq(), daughter.Ysq(), 0, (float) ((daughter.rho*1.0 / G().MAX_RHO) * 0.5 + 0.5), 0);
//            }
        }
    }

    void SetDiffusibles(){
        if (IsNecroticNonStem()||IsNecroticStem()) {
//            G().tnfa.Add(Xsq()+1,Ysq()+1,Zsq() + 1, G().TNFA_PRODUCTION_RATE);//FIXME- go back to this line of code when ADI solver is introduced
//            G().tnfa.Add(Xsq()+1,Ysq()+1,Zsq()+1, G().TNFA_PRODUCTION_RATE);//Isq() index/position of cell (not i,j form)ADD 1 BECAUSE OF ADI DIFFUSION
            G().tnfa.Add(Xsq(),Ysq(),Zsq(), G().TNFA_PRODUCTION_RATE);//FIXME- go back to this line of code when ADI solver is introduced
            G().tnfa.Add(Xsq(),Ysq(),Zsq(), G().TNFA_PRODUCTION_RATE);//Isq() index/position of cell (not i,j form)ADD 1 BECAUSE OF ADI DIFFUSION
        }else{
//            double myO2 = G().o2.GetPix(Xsq()+1,Ysq()+1, Zsq()+1);//Isq() gets the current indices position of the cell (not i,j coordinates)
            double myO2 = G().o2.Get(Xsq(),Ysq(), Zsq());//Isq() gets the current indices position of the cell (not i,j coordinates)
//            double myO2 = G().o2.GetPix(Xsq(),Ysq(), Zsq());//Isq() gets the current indices position of the cell (not i,j coordinates)
//            G().o2.SetColor(Xsq()+1,Ysq()+1,Zsq()+1, myO2>G().O2_CONSUMPTION_RATE?myO2-G().O2_CONSUMPTION_RATE:0);//FIXME- go back to this line of code when ADI solver is introduced
            G().o2.Set(Xsq(),Ysq(),Zsq(), myO2>G().O2_CONSUMPTION_RATE?myO2-G().O2_CONSUMPTION_RATE:0);//FIXME- go back to this line of code when ADI solver is introduced
//            G().o2.SetColor(Xsq(),Ysq(),Zsq(), myO2>G().O2_CONSUMPTION_RATE?myO2-G().O2_CONSUMPTION_RATE:0);//statement?if lacking o2, o2 set to 0:if false
        }
    }
    boolean OnlyStemCellSimulation() {
        if(G().STEM_SYMMETRIC_DIV == 1){
            return true;
        }
        return false;
    }
    void Step() {//GuiGridVis visCells removed 7/13- no 3d visualization
        isHypoxic = false;
        if (IsNecroticNonStem()|| IsNecroticStem()) {
            if(G().GetTick()-necroticTick>=G().DECOMPOSITION_TIME){//necrotic tick-time cell became necrotic
//                visCells.SetColor(Xsq(), Ysq(), 1, 1, 1);
//                if(IsNecroticStem() && !OnlyStemCellSimulation()){
//                    SetNbhdColor(visCells, 1,1,1);
//                }
                G().nNecroticCells--;//decrement as cells are disposed of
                Dispose();
                return;
            }
//            visCells.SetColor(Xsq(), Ysq(), (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), 0, 0);
//            if(IsNecroticStem() && !OnlyStemCellSimulation()){
//                SetNbhdColor(visCells, (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), 0, 0);
//            }
        }
        else {
            if(G().rn.nextDouble()<G().APOP_PROB){//random double 0-1
                if(!IsStem()){
//                    visCells.SetColor(Xsq(),Ysq(),1,1, 1);
                    G().nNonStemCells--;
                }
                if(IsStem() && !OnlyStemCellSimulation()){
//                    SetNbhdColor(visCells, 1, 1, 1);
                    G().nStem--;
                }
                Dispose();
                return;
            }
//            double myTNFA=G().tnfa.GetPix(Xsq()+1,Ysq()+1, Zsq()+1);
//            double myO2 = G().o2.GetPix(Xsq()+1,Ysq()+1, Zsq() + 1);//FIXME-use this line of code when ABM solver introduce
            double myTNFA=G().tnfa.Get(Xsq(),Ysq(), Zsq());
            double myO2 = G().o2.Get(Xsq(),Ysq(), Zsq());//FIXME-use this line of code when ABM solver introduce
//            double myO2 = G().o2.GetPix(Xsq(),Ysq(), Zsq());//Isq() gets the current indices position of the cell (not i,j coordinates)
            if (myTNFA> G().NECROTIC_TNFA || myO2 < G().NECROTIC_OXYGEN) {//FIXME-add Xsq + 1 (for xyz) when ABM solver is implemented
                G().nNecroticCells++;
                necroticTick=G().GetTick();
                if(!IsStem()) {
                    rho = -2;//Necrotic non-stem cell
//                    visCells.SetColor(Xsq(), Ysq(), (float) ((G().GetTick() - necroticTick) * 0.7 / G().DECOMPOSITION_TIME + 0.3), 0, 0);
                    G().nNonStemCells--;
                }
                if(IsStem()){
                    rho = -3;//is necrotic stem
                    G().nStem--;
//                    if(!OnlyStemCellSimulation()){
//                        SetNbhdColor(visCells,(float)((G().GetTick()-necroticTick)*0.7/G().DECOMPOSITION_TIME+0.3),0,0);
//                    }
                }
                return;
            }
            if (myO2 < G().QUIESCENT_OXYGEN || myTNFA > G().QUIESCENT_TNFA ) {//FIXME- or if lvltnfa < QUIESENT_LEVEL
                G().nHypoxic++;
                isHypoxic = true;
//                visCells.SetColor(Xsq(), Ysq(), (float).5, (float).5, (float).5);
                if(IsStem() && !OnlyStemCellSimulation()) {
//                    SetNbhdColor(visCells, .5f, .5f, .5f);
                }
                return;
            }
            cellCycleTime++;
            //ISSURROUNDED?
//            if(FindEmptyNeighbors3d(G().mooreHood,G().localIs,Xsq(),Ysq(),Zsq(),false, false, false) == 0) {//G().calls functions from corresponding Grid class
//                isSurrounded = true;
//                return;
//            } else{
//                isSurrounded = false;
//            }
            if (cellCycleTime % G().NUM_ITER_BETWEEN_MOVEMENT == 0) {//moves 48 times a day
//                MoveSQ(visCells);
                Move();
            }
            if (cellCycleTime >= G().ITERATIONS_PER_DAY) {
//                Reproduce(visCells);//FIXME-if cell is disposed of and returns, rest of function still executed
                Reproduce();
                //ADDED 6/29 9:12
                if(!Alive()){//if cell dies because rho==0
                    return;
                }
            }
//            if (IsStem() && !OnlyStemCellSimulation()) {
//                SetNbhdColor(visCells, 0,0,1);//change to blue
//            }else if(IsStem()){
//                visCells.SetColor(Xsq(), Ysq(), 0,0,1);
//            } else{
//                visCells.SetColor(Xsq(), Ysq(), 0, (float) ((rho*1.0/ G().MAX_RHO) * 0.5 + 0.5), 0);
                //visCells.SetColorBound(Xsq(), Ysq(), 0, 1, 0);//FIXME gradient
//            }
        }
    }
}

public class ABM3D2 extends AgentGrid3D<ABMcell3d2> {
    //globals
    final int TIMESTEP_MS=0;
    static final int GRID_SIZE=200;//switched name from GRID_SIZE to GRID_SIZE
    static final int ITERATIONS_PER_DAY=24 * 4;//once every 6 min 96 was original val
    static final int NUM_DAYS = 20;
    static final int TIMESTEPS=ITERATIONS_PER_DAY*NUM_DAYS +1;//+1 ensures we get the last cell data written to a file
    static final int PIXEL_SIZE=2;
    static final double STEM_SYMMETRIC_DIV=1;
//    static final boolean OnlyStemCellSimulation = STEM_SYMMETRIC_DIV == 1?true:false; //replace function with this to make code more readable/editable

    //CA CONSTANTS
    static final int DIFF_TIMESTEP= 1;
    final int MAX_RHO=7;
    final int CELL_WIDHT=20;
    final double NECROTIC_TNFA=0.0000385;//OK
    final double NECROTIC_OXYGEN=0.0001;
    final double QUIESCENT_OXYGEN=0.0035;//mM
    final double QUIESCENT_TNFA = .00001925;//HALF NECROTIC LEVEL FOR TNFA
    static final double TNFA_PRODUCTION_RATE=0.000005*DIFF_TIMESTEP;
    final int DAYS_BEFORE_DECOMPOSITION = 20;//?
    final int DECOMPOSITION_TIME=ITERATIONS_PER_DAY * DAYS_BEFORE_DECOMPOSITION;
    final double APOP_PROB=ProbScale(0,ITERATIONS_PER_DAY);//PROB SCALE ACCOUNTS FOR MULTIPLE ITERATIONS IN A DAY
    final int SEC_PER_DAY = 24 *3600;


    //DIFFUSIBLE CONSTANTS
    //FIXME- DIFFUSION IS UNSTABLE EVEN WITH VERY LOW DIFFUSION IF O2 IS CONSUMED
//    final int DIFF_TIMESTEP= (SEC_PER_DAY)/ITERATIONS_PER_DAY;//num seconds per iteration
    //PARAMETER THAT CHANGES OUTPUT FILENAME
    static final double O2_INCREASE_RATIO = 1;
    static final double DEC_RATE_DIFFUSION = 22;

    static final double O2_BOUNDARY_CONDITION=0.07;//mM
    final double TNFA_BOUNDARY_CONDITION=0;
//    final double O2_DIFF_RATE=(1460.0/(CELL_WIDHT*CELL_WIDHT))*DIFF_TIMESTEP*O2_INCREASE_RATIO;//micrometers squared/sec um^2/sec. needs to be float/double
    final double O2_DIFF_RATE=(1460/(DEC_RATE_DIFFUSION*CELL_WIDHT*CELL_WIDHT))*DIFF_TIMESTEP;//micrometers squared/sec um^2/sec. needs to be float/double
    final double TNFA_DIFF_RATE=(100.0/(DEC_RATE_DIFFUSION*CELL_WIDHT*CELL_WIDHT))*DIFF_TIMESTEP;//FIXME-time step length
    final double TNFA_DEGREDATION_RATE=ProbScale((.01), SEC_PER_DAY / ITERATIONS_PER_DAY);//.00001 hz
//    final double O2_CONSUMPTION_RATE=0.012/DIFF_ADJUSTMENT_CONSTANT_O2*DIFF_TIMESTEP;//mM/s//FIXME-replace realistic value- using lower val for now
    static final double O2_CONSUMPTION_RATE=.036/(DEC_RATE_DIFFUSION * DIFF_TIMESTEP);//mM/s//FIXME-replace realistic value- using lower val for now
    final double O2_MAX_DIFF_THRESH=0.0001;
    final double TNFA_MAX_DIFF_THRESH=0.0001;
    //ADDED NEXT 3 LINES 6/28 6:43
    final double GRID_SQUARE_SIZE = 400;//um^2
    final double CELL_MOVEMENT_SPEED = .2;//uM/min-avgs out to every 30-60 min
    //final int CELL_MOVEMENTS_PER_DAY = (i
    final int CELL_MOVEMENTS_PER_DAY = 24;
    final int NUM_ITER_BETWEEN_MOVEMENT = ITERATIONS_PER_DAY/CELL_MOVEMENTS_PER_DAY;//cell moves every 30 min

    final TickTimer trt=new TickTimer();
//    final GuiGridVis visCells;
    final GuiGridVis visO2;
    final GuiGridVis visTNFA;
    final GuiLabel cellcts;//just a label for GUI (self explanatory)
    final int[] mooreHood=MooreHood3d(false);
    final int[] localIs=new int[mooreHood.length/3];//FIXME-check length of localis if not working

    //typeGrid components
    final PDEGrid3D o2;
    final PDEGrid3D tnfa;
    final PDEGrid2D o2Slice;
    final PDEGrid2D tnfaSlice;
    final double[] steadyCheckO2;
    final double[] steadyCheckTnfa;
    final Random rn=new Random();
    FileIO out;//changed to final

    int nStem;
    int nNecroticCells;
    int nNonStemCells;
    int nHypoxic;
    int numViable = nStem + nNonStemCells;
    String outPath;

    ABM3D2(int x, int y, int z, String outPath, GuiGridVis visO2, GuiGridVis visTNFA, GuiLabel cellcts) {// FIXME-7/13 these var were deleted when switched to 3d (no imaging yet) GuiLabel cellCounts, GuiGridVis visCells, GuiGridVis visO2, GuiGridVis visTNFA,GuiLabel cellcts,
        super(x,y,z,ABMcell3d2.class);
//            o2=new PDEGrid3D(x+2,y+2, z + 2);//FIXME-replace this and next line when ABM solver implemented
//            tnfa=new PDEGrid3D(x+2,y+2, z + 2);//define 2 new grids in the "master" ABM3D class
            o2=new PDEGrid3D(x,y, z );//FIXME-replace this and next line when ABM solver implemented
            tnfa=new PDEGrid3D(x,y, z);//define 2 new grids in the "master" ABM3D class
            o2Slice = new PDEGrid2D(yDim, zDim);
            tnfaSlice = new PDEGrid2D(yDim, zDim);
            this.outPath = outPath;
//            o2=new PDEGrid3D(x,y,z);
//            tnfa=new PDEGrid3D(x,y,z);//define 2 new grids in the "master" ABM3D class
            //initialize typeGrid
            ABMcell3d2 first= NewAgentSQ(xDim/2,yDim/2, zDim/2);//xDim yDim passed in as x,y I think? From parent class
            first.Init(MAX_RHO,true);//create Stem cell
            //define above with constants
            nNecroticCells=0;
            nStem=1;
            nNonStemCells=0;
            nHypoxic=0;



        Arrays.fill(o2.GetField(),O2_BOUNDARY_CONDITION);//FIXME- does o2.field get the correct value?
        Arrays.fill(tnfa.GetField(),TNFA_BOUNDARY_CONDITION);//
            o2.SetAll(O2_BOUNDARY_CONDITION);//FIXME-reinstate when ABM solver is implemented
            tnfa.SetAll(TNFA_BOUNDARY_CONDITION);
//            this.visCells=visCells;
            this.visO2=visO2;
            this.visTNFA=visTNFA;
            this.steadyCheckO2=new double[o2.length];
            this.steadyCheckTnfa=new double[tnfa.length];
            this.cellcts=cellcts;
            if(outPath!=""){
                out=new FileIO(outPath,"w");
                out.Write("X Val,Y Val,Z Val,Rho,NecroticTick\n");
            } else{ out=null; }
        }

    double GetMaxDiff(double[] a,double[] b){
        double max=Double.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            max=Math.max(Math.abs(a[i]-b[i]),max);
        }
        return max;
    }
    String GetModelData(int i){//changed to public MODIFIED 7/13 late night//FIXME- reimplement if GUI is implemented
        return "Day: "+(int)((i / ITERATIONS_PER_DAY))+" Live Cells: "+numViable+" Stem Cells: "+nStem+" Non-Stem Cells:"+nNonStemCells+" Necrotic:"+nNecroticCells+" Hypoxic:"+nHypoxic;
//        String returnVal;
//        for (ABMcell3d c: this) {
//            returnVal += c.Xsq()+","+c.Ysq()+","+c.Zsq()+","+tnfa.GetPix(c.Isq())+","+o2.GetPix(c.Isq())+","+c.rho+"\n";//x,y,z,o2conc,tnfaconc,rho==cellstatus(stem,nonstem,necrotic)
//        }
//        return returnVal;
    }

    public void SetOuterLayerCurr(PDEGrid3D grid, double val){
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < xDim; y++) {//ASK RAFAEL- why in Dif2d is it (y < yDim -1) not y < yDim?
                grid.Set(x, y, 0, val);
                grid.Set(x, y, yDim - 1, val);
            }
        }
        for (int x = 0; x < xDim; x++) {
            for (int z = 0; z < xDim; z++) {
                grid.Set(x, 0, z, val);
                grid.Set(x, yDim - 1, z, val);
            }
        }
        for (int y = 0; y < xDim; y++) {
            for (int z = 0; z < xDim; z++) {
                grid.Set(0, y, z, val);
                grid.Set(xDim - 1, y, z, val);
            }
        }
    }
    void WriteModelDataToFile(int currentDay){
        numViable= nStem + nNonStemCells;
//        out.Write(numViable+","+nStem+","+nNonStemCells+","+nNecroticCells+","+nHypoxic+"\n");
        //write out coordinates
        out.Write("Day "+currentDay+",New Day,,,\n");
        for(ABMcell3d2 c: this) {
            int outputRho = c.rho + 100;//R won't accept negative color vals
            if(c.isHypoxic){
                int rho = -5;//next lowest number after -4 for stem cells stem cell only simulation
                outputRho = rho + 100;
            }
            //System.out.println(outputRho);
            out.Write(c.Xsq()+","+c.Ysq()+","+c.Zsq()+","+outputRho+","+c.necroticTick+"\n");
        }
    }
    boolean DiffusionStep(){//FIXME- 7/15 replaced with OneDiffusionStep
        //store current values
        System.arraycopy(o2.GetField(),0,steadyCheckO2,0,o2.length);
        System.arraycopy(tnfa.GetField(),0,steadyCheckTnfa,0,o2.length);
        for (ABMcell3d2 c : this) {
            c.SetDiffusibles();
        }
        for (int i = 0; i < tnfa.length; i++) {
            tnfa.Set(i,tnfa.Get(i) - tnfa.Get(i)*TNFA_DEGREDATION_RATE);//FIXME change to a lower rate
        }
//        o2.SetOuterLayer(O2_BOUNDARY_CONDITION);//FIXME-when ADI3 is implemented, uncomment this, also change code back to typeGrid.size +2 and x + 1 (when initializing and setting vals)
//        tnfa.SetOuterLayer(TNFA_BOUNDARY_CONDITION);
        o2.Diffusion(O2_DIFF_RATE);
        SetOuterLayerCurr(o2, O2_BOUNDARY_CONDITION);//FIXME-when ADI3 is implemented, uncomment this, also change code back to typeGrid.size +2 and x + 1 (when initializing and setting vals)
        SetOuterLayerCurr(tnfa,TNFA_BOUNDARY_CONDITION);
        tnfa.Diffusion(TNFA_DIFF_RATE);
        //compare previous values to current values
        double maxO2diff=GetMaxDiff(o2.GetField(),steadyCheckO2);
        double maxTnfadiff=GetMaxDiff(tnfa.GetField(),steadyCheckTnfa);
        if(maxO2diff<O2_MAX_DIFF_THRESH&&maxTnfadiff<TNFA_MAX_DIFF_THRESH){
            return true;
        }
        return false;
//        return true;//FIXME-change back to return false-this is just a test
    }

    void OneDiffusionStep(){
        //store current values
        System.arraycopy(o2.GetField(),0,steadyCheckO2,0,o2.length);
        System.arraycopy(tnfa.GetField(),0,steadyCheckTnfa,0,o2.length);
        for (ABMcell3d2 c : this) {
            c.SetDiffusibles();
        }
        for (int i = 0; i < tnfa.length; i++) {
            tnfa.Set(i,tnfa.Get(i) - tnfa.Get(i)*TNFA_DEGREDATION_RATE);//FIXME change to a lower rate
        }
        for(int i = 0; i < DEC_RATE_DIFFUSION; i++) {
//            SetOuterLayer(o2, O2_BOUNDARY_CONDITION);//FIXME-when ADI3 is implemented, uncomment this, also change code back to typeGrid.size +2 and x + 1 (when initializing and setting vals)
//            SetOuterLayer(tnfa,TNFA_BOUNDARY_CONDITION);
            o2.Diffusion(O2_DIFF_RATE, O2_BOUNDARY_CONDITION);
            tnfa.Diffusion(TNFA_DIFF_RATE, TNFA_BOUNDARY_CONDITION);
        }
        //compare previous values to current values
        double maxO2diff=GetMaxDiff(o2.GetField(),steadyCheckO2);
        double maxTnfadiff=GetMaxDiff(tnfa.GetField(),steadyCheckTnfa);
        if(maxO2diff > 1 || maxTnfadiff > 1){
            System.out.println("ERROR-DIFFUSION UNSTEADY(max_diff>1)");
        }
//        System.out.println("GetMax o2 diff is " +GetMaxDiff(o2.field, steadyCheckO2));
//        System.out.println("GetMax tnfa diff is " +GetMaxDiff(tnfa.field, steadyCheckTnfa));
//        if(maxO2diff<O2_MAX_DIFF_THRESH&&maxTnfadiff<TNFA_MAX_DIFF_THRESH){
//            return true;
//        }
//        return false;
//        return true;//FIXME-change back to return false-this is just a test
    }
    public int[] MooreHood3d(boolean includeOrigin) {
        if (includeOrigin) {
            return new int[]{0, 0, 0,
                    0, 0, 1,
                    0, 0, -1,
                    1, 0, 0,
                    1, 0, 1,
                    1, 0, -1,
                    1, 1, 0,
                    1, 1, 1,
                    1, 1, -1,
                    0, 1, 0,
                    0, 1, 1,
                    0, 1, -1,
                    -1, 0, 0,
                    -1, 0, 1,
                    -1, 0, -1,
                    -1, 1, 0,
                    -1, 1, 1,
                    -1, 1, -1,
                    -1, -1, 0,
                    -1, -1, 1,
                    -1, -1, -1,
                    0, -1, 0,
                    0, -1, 1,
                    0, -1, -1,
                    1, -1, 0,
                    1, -1, 1,
                    1, -1, -1,
            };
        } else {
            return new int[]{
                    0, 0, 1,
                    0, 0, -1,
                    1, 0, 0,
                    1, 0, 1,
                    1, 0, -1,
                    1, 1, 0,
                    1, 1, 1,
                    1, 1, -1,
                    0, 1, 0,
                    0, 1, 1,
                    0, 1, -1,
                    -1, 0, 0,
                    -1, 0, 1,
                    -1, 0, -1,
                    -1, 1, 0,
                    -1, 1, 1,
                    -1, 1, -1,
                    -1, -1, 0,
                    -1, -1, 1,
                    -1, -1, -1,
                    0, -1, 0,
                    0, -1, 1,
                    0, -1, -1,
                    1, -1, 0,
                    1, -1, 1,
                    1, -1, -1,
            };
        }
    }

    void Step(int i){
        trt.TickPause(TIMESTEP_MS);//increase TIMESTEP_MS if program is too fast
        nHypoxic=0;//get the per-step number of quiescent cells rather than a running total
        for (ABMcell3d2 c : this) {//this refers to all members of ABMcell3d class
        c.Step();//LOOKUP c.StepModules function
        }//set a return bool value and plug return bool val into next for loop (necrotic cells don't consume O2)
        CleanShuffInc(rn);//Clean moves unoccupied agents to end of the array, inc increments timer
        int ct=0;
//        while(DiffusionStep()){//FIXME- 7/15 replaced with OneDiffusionStep
//            ct++;
//        }
        for(int j = 0; j < 1; j++){//DIFF_ADJUSTMENT_CONSTANT;j++){//FIXME- reimplement diffusion here
            OneDiffusionStep();
        }
        //DRAW DIFFUSIBLE ADDED 7/19 11:30am
//        Update2dDiffGrid(o2Slice, o2);
//        Update2dDiffGrid(tnfaSlice, tnfa);
        DrawDiffusible(visO2,o2Slice,O2_BOUNDARY_CONDITION);//o2_boundary_condition is the MAX param
        DrawDiffusible(visTNFA,tnfaSlice,NECROTIC_TNFA);//

        this.cellcts.SetText(GetModelData(i));
//        if(out!=null) { WriteModelDataToFile(); }
    }
    //FIXME make 3d

    void Update2dDiffGrid(PDEGrid2D updateMe, PDEGrid3D getDataFromMe){
        updateMe.SetAll(0);
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                for (int z = 0; z < zDim; z++) {
                    updateMe.Add(x,y,(getDataFromMe.Get(x,y,z)/zDim));
//                    double avgRowVal = 0;
//                    avgRowVal += getDataFromMe.GetPix(x,y,z);//gets avg val of diffusion constants for respective row
//                    avgRowVal = avgRowVal / zDim;
                }
//                updateMe.SetColor(x,y,avgRowVal);
            }
        }
    }

    void DrawDiffusible(GuiGridVis drawHere, PDEGrid2D drawMe, double max){//max is the max val of a certain variable eg. max.rho or max tic-necrotic.tic
        double maxVal=Double.MIN_VALUE;
        double minVal=Double.MAX_VALUE;
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                maxVal=Math.max(maxVal,drawMe.Get(x,y)/max);
                minVal=Math.min(minVal,drawMe.Get(x,y)/max);
                drawHere.SetPix(x, y, Utils.HeatMapRGB(drawMe.Get(x,y)));
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
//        GuiGridVis visCells=new GuiGridVis(ABM3D.GRID_SIZE,ABM3D.GRID_SIZE,PIXEL_SIZE);//FIXME set to 2
//        for(int i = 0; i < GRID_SIZE; i++){
//            for(int j = 0; j < GRID_SIZE; j++){
//                visCells.SetColor(i,j,1,1,1);
//            }
//        }
        GuiGridVis visO2=new GuiGridVis(ABM3D2.GRID_SIZE,ABM3D2.GRID_SIZE,PIXEL_SIZE);
        GuiGridVis visTNFA=new GuiGridVis(ABM3D2.GRID_SIZE,ABM3D2.GRID_SIZE,PIXEL_SIZE);
        GuiLabel cellCts=new GuiLabel("|",3,1);//initialized with |, spans all 3 columns (3)?,
        ABM3D2 model=new ABM3D2(ABM3D2.GRID_SIZE,ABM3D2.GRID_SIZE,ABM3D2.GRID_SIZE,
                ("outFile.+TNFAProduction"+TNFA_PRODUCTION_RATE+"O2Consumptionx"+O2_CONSUMPTION_RATE+"O2BoundVal"+O2_BOUNDARY_CONDITION+".csv"),
                visO2, visTNFA, cellCts);//deleted 7/13 until visualization works out: visO2,visTNFA,cellCts,
        GuiWindow win=new GuiWindow("StaticModel Visualization",true);//new window
        win.AddCol(0, cellCts);
        //IF CELL VIS WORKS 3D
//        win.AddCol(new GuiLabel("Stem: Blue, Normal: Green, Hypoxic: Grey, Necrotic: Red"),0);//adding a column automatically resizes the window-3 columns are added here
//        //each col filled by col (top-> bottom)
//        win.AddCol(visCells,0);
//        win.AddCol(new GuiLabel("O2 Conc"),1);
//        win.AddCol(visO2,1);
//        win.AddCol(new GuiLabel("TNFA Conc"),2);
//        win.AddCol(visTNFA,2);
        //
        win.AddCol(0, new GuiLabel("O2 Conc"));
        win.AddCol(0, visO2);
        win.AddCol(1, new GuiLabel("TNFA Conc"));
        win.AddCol(1, visTNFA);
        win.RunGui();
        for (int i = 0; i < ABM3D2.TIMESTEPS ; i++) {
            model.Step(i);
            //ADDED 7/7
            if((i % (ITERATIONS_PER_DAY)) == 0){
                model.cellcts.SetText(model.GetModelData(i));
                int currentDay = i / ITERATIONS_PER_DAY;
                if(model.out !=null) {
                    model.WriteModelDataToFile(currentDay);
                    if(model.out!=null){
                        model.out.Close();
                        model.out=new FileIO(model.outPath,"a");
                    }
                }
                System.out.println("Day" + currentDay);
//                if(ABMcell..nNecroticCells > 1){
//                System.out.println("Necrotic Cells exist");
            }
            if((i == (ITERATIONS_PER_DAY * 5) )) {//
//                visCells.ToGIF("Cells Day"+currentDay+".png");
            }
        }
        if(model.out!=null){ model.out.Close();}
        System.out.println("Simulation completed in ");
        win.Dispose();
    }
}
