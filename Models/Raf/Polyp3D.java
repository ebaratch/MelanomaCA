package Models.Raf;

import Framework.GridsAndAgents.AgentSQ3Dunstackable;
import Framework.GridsAndAgents.AgentGrid3D;
import Framework.GridsAndAgents.PDEGrid3D;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiWindow;
import Framework.Gui.Vis3DOpenGL;
import Framework.Tools.FileIO;
import Framework.Gui.TickTimer;

import java.util.ArrayList;
import java.util.Random;

import static Framework.Utils.*;

/**
 * Created by bravorr on 6/15/17.
 */

//18mm diameter 1mm depth
    //2x2x1
class Polyp3D extends AgentGrid3D<Cell3D> {
    public final static int BLACK=RGB(0,0,0);
    final PDEGrid3D resource;
    final int[] divLocs;
    final int[] divIs;
    final Random rn;
    TickTimer trt;
    final double cellCycleStart =2.5;
    final double cellCycleWiggle =1;
    final double consumptionMax =0.1;
    final double consumptionHalfMaxConc=0.2;
    final double diffRate =0.2;
    final double deathConcMax =0.01;
    final double deathConcMin =0.0001;
    final double deathConcRange=deathConcMax-deathConcMin;
    final long tickRate=0;
    final int initDiffSteps=1000;
    final int timeSteps=100000;
    final boolean gui;
    final GuiWindow win;
    final Vis3DOpenGL vis3D;
    final ArrayList<GuiGridVis> viss;
    final double everyWhereSourceVal=0.001;
    public Polyp3D(int x, int y, int z,boolean gui) {
        super(x, y, z, Cell3D.class);
        resource=new PDEGrid3D(x,y,z);
        divLocs= VonNeumannHood3D(false);
        divIs=new int[divLocs.length/3];
        rn=new Random();
        trt=new TickTimer();
        this.gui=gui;
        if(gui){
            vis3D =new Vis3DOpenGL("3D View", 1000,1000,x,y,z, true);
            win=new GuiWindow("disp",true);
            viss=new ArrayList<>();
            int nCols=(int)Math.ceil(Math.sqrt(yDim/2));
            for (int i = 0; i <yDim/2  ; i++) {
                GuiGridVis vis=new GuiGridVis(xDim,zDim,2);
                viss.add(vis);
                win.AddCol(i/nCols, vis);
            }
            win.RunGui();
        }
        else{
            vis3D =null;
            win=null;
            viss=null;
        }
    }
    void Init(int nCells,double includeProp){
        for (int i = 0; i < nCells; i++) {
            int x=rn.nextInt((int)(xDim*includeProp))+(int)(xDim*(1-includeProp)/2);
            int y=rn.nextInt((int)(yDim*includeProp))+(int)(yDim*(1-includeProp)/2);
            int z=rn.nextInt((int)(zDim*includeProp))+(int)(zDim*(1-includeProp)/2);
            NewAgentSQ(x,y,z);
        }
        resource.SetAll(1);
    }
    void Step(){
        trt.TickPause(tickRate);
        for (Cell3D c : this) {
            c.Step();
        }
        CleanShuffInc(rn);
        if(everyWhereSourceVal!=0){
            for (int i = 0; i < resource.length; i++) {
                resource.Add(i,everyWhereSourceVal);
            }
            resource.BoundAll(0,1);
            resource.Diffusion(0.1, false, false, false);
        }
        else {
            resource.Diffusion(0.1, (double) 1, false, false, false);
        }
    }
    void Record1() {
        System.out.println("recording "+ GetTick());
        FileIO out=new FileIO("Polyp3D_"+ GetTick()+".csv","w");
        out.Write("x,y,z,cell,diffusible,cycle\n");
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                for (int z = 0; z < zDim; z++) {
                    Cell3D c=GetAgent(x,y,z);
                    if(c==null) {
                        out.Write(x + "," + y + "," + z + ",0,"+resource.Get(x,y,z)+",0\n");
                    }
                    else{
                        out.Write(x + "," + y + "," + z + ",1,"+resource.Get(x,y,z)+","+c.cellCycle+"\n");
                    }
                }
            }
        }
        out.Close();
    }
    void Record2(){
        System.out.println("recording "+ GetTick());
        FileIO out=new FileIO("Polyp3Dcells_"+ GetTick()+".csv","w");
        out.Write("x,y,z,cell,diffusible,cycle\n");
        for (Cell3D c : this) {
            out.Write(c.Xsq() + "," + c.Ysq() + "," + c.Zsq() + ",1,"+resource.Get(c.Xsq(),c.Ysq(),c.Zsq())+","+c.cellCycle+"\n");
        }
        out.Close();
        out=new FileIO("Polyp3Ddiff_"+ GetTick()+".csv","w");
        for (int x = 0; x < xDim; x++) {
            for (int z = 0; z < zDim; z++) {
                double sum=0;
                for (int y = 0; y < yDim; y++) {
                    sum+=resource.Get(x,y,z);
                }
                if(z==zDim-1){
                    out.Write(sum+"");
                }
                else{
                    out.Write(sum+",");
                }
            }
            out.Write("\n");
        }
        out.Close();
    }

    void Draw2D(){
        for (int y = 0; y < yDim/2; y++) {
            GuiGridVis vis=viss.get(y);
            for (int x = 0; x < xDim; x++) {
                for (int z = 0; z < zDim; z++) {
                    float val=(float) Bound(resource.Get(x,y*2,z),0,1);
                    double r = GetAgent(x,y*2,z)==null?0:1;
                    vis.SetPix(x, z, RGB(r, (double) val, (double) 0));
                    //vis3D.SetColor(x,z,GetAgent(x,y,z)==null?0:1,0,0);
                }
            }
        }
    }
    void Draw3D(){
        vis3D.Clear(BLACK);
        for (Cell3D c : this) {
            vis3D.CelSphere(c.Xpt(),c.Ypt(),c.Zpt(),1,SetBlue(HeatMapRGB(Math.pow(resource.Get(c.Isq()),1.0/3)),1.0-c.cellCycle/(cellCycleStart+cellCycleWiggle)));
            //vis3D.Circle(c.Xpt(),c.Ypt(),c.Zpt(),1,SetBlue(HeatMapRGB(Math.pow(resource.Get(c.Isq()),1.0/3)),1.0-c.cellCycle/(cellCycleStart+cellCycleWiggle)));
        }
        vis3D.Show();
    }
    void AddCell(int iSq){
        Cell3D child= NewAgentSQ(iSq);
        child.ResetCellCycle();
    }
    void AddCell(int x,int y,int z){
        AddCell(I(x,y,z));
    }
    public static void main(String[] args){
        //dimensions of vat
        Polyp3D p=new Polyp3D(100,50,49,true);
        p.Init(5,0.8);
        for (int i = 0; i < p.timeSteps && !p.vis3D.CheckClosed() ; i++) {
            p.Step();
            if(p.gui) {
                p.Draw2D();
                p.Draw3D();
            }
        }
        if(p.gui){
            p.vis3D.Dispose();
            p.win.Dispose();
        }
    }
}
class Cell3D extends AgentSQ3Dunstackable<Polyp3D> {
    double cellCycle;
    void ResetCellCycle(){
        cellCycle=G().cellCycleStart +G().rn.nextDouble()*G().cellCycleWiggle;
    }
    void Step(){
        if(UseResource()){
            return;
        }
        if(cellCycle<0){
            Divide();
        }
    }
    boolean UseResource(){
        //returns true if death from lack of nutrients occurs
        double currRes=G().resource.Get(Isq());
        double resVal=MichaelisMenten(G().resource.Get(Isq()),G().consumptionMax,G().consumptionHalfMaxConc);
        G().resource.Add(Isq(),-resVal);
        if(resVal<G().deathConcMax){
            if(resVal<G().deathConcMin||G().rn.nextDouble()<(resVal-G().deathConcMin)/G().deathConcRange){
                if(G().gui){
                    G().viss.get(Ysq()/2).SetPix(Xsq(), Zsq(), RGB((double) 0, (double) 0, (double) 0));
                }
                Dispose();
                return true;
            }
        }
        cellCycle-=resVal;
        return false;
    }
    boolean Divide(){
        //returns true if division occurs
        //cell cycle gets reset whether cell divides or not
        int nIs=G().HoodToIs(G().divLocs,G().divIs,Xsq(),Ysq(),Zsq(),false,false,false);
        int nHits=0;
        for (int i = 0; i < nIs; i++) {
            if(G().GetAgent(G().divIs[i])==null){
                G().divIs[nHits]=G().divIs[i];
                nHits++;
            }
        }
        if(nHits==0){
            return false;
        }
        ResetCellCycle();
        int iDiv=G().divIs[G().rn.nextInt(nHits)];
        G().AddCell(iDiv);
        return true;
    }
}

