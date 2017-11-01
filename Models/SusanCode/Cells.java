package Models.SusanCode;

import Framework.GridsAndAgents.AgentSQ2D;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GuiGridVis;

import java.util.Random;

import static Models.SusanCode.CellTypes.*;

class Cells extends AgentSQ2D<ModelGrid>
{
    PDEGrid2D TC(){
        return G().tumorChemo;
    }
    int cellType;
    int cellDirection;
    boolean isLevy;
    int count;
    boolean start;
    int size;
    int TCK;
    double vertProb;
    double horiProb;
    int speed;
    int numDivis;
    int tdCount;
    int killTime;
    boolean tStart;
    double curr;
    int tKillTime;
    int prolif;

    boolean CalcDirection() {

        int x = Xsq();
        int y = Ysq();
        PDEGrid2D myGradient = null;
        if (getType() == TCELLi || (getType() == DCELLm) || (getType() == TCELLa && getTDCount() > 0) || getType() == TCELLpi) // FRC CCL 19/21
        {
            myGradient = G().frcCCL1921;
        } else if ((getType() == TCELLa && getTDCount() <= 0) || getType() == DCELLi) {
            myGradient = G().tumorChemo;
        } else if (getType() == BCELLi || getType() == BCELLa || getType() == DCELLf) {
            myGradient = G().frcCXCL13;
        } else {
            return false;//not calculating direction
        }
        if (getType() == TCELLa) {
            setTDCount(getTDCount() - G().timeStep);// tumor death count
        }
//        int max = G().findMax(
//                myGradient.GetPix(x, y + 1),
//                myGradient.GetPix(x, y - 1),
//                myGradient.GetPix(x - 1, y),
//                myGradient.GetPix(x + 1, y),
//                myGradient.GetPix(x, y)); // checks for the highest chemokine
        // concentration
        double vert;
        double hori;
        double mappingProb;
        double vertProb;
        double horiProb;
        //if (myGradient.GetPix(x, y) >= 0.00005) // if there is a great enough chemokine gradient concentration where the cell is currently located
            if (myGradient.Get(x, y) >= G().chemThreshold) // if there is a great enough chemokine gradient concentration where the cell is currently located
            {
            if (myGradient == G().frcCCL1921) {
                G().consumeCCL(x, y, G().chemConsumption,G().timeStep);
            }
            else if (myGradient == G().frcCXCL13)
            {
                G().consumeCXCL(x,y,G().chemConsumption,G().timeStep);
            }
            else if (myGradient == G().tumorChemo)
            {
                G().consumeTumorGrad(x,y,G().chemConsumption,G().timeStep);
            }
            if (getType() == BCELLi) // Inactive B-cells increase chemokinesis/searching
            {
                setSpeed(3);
            }
            vert = G().subtract(myGradient.Get(x, y + 1),
                    myGradient.Get(x, y - 1));
            hori = G().subtract(myGradient.Get(x + 1, y),
                    myGradient.Get(x - 1, y));
            mappingProb = G().cellMapping(G().cellMagnitude(vert, hori), .1); // .1 = kind of randomly picked upper bound for now
            vertProb = vert / (Math.abs(vert) + Math.abs(hori)); // positive
            // if up,
            // negative
            // if down
            horiProb = hori / (Math.abs(vert) + Math.abs(hori)); // positive
            // if right,
            // negative
            // if left
            setDirection(G().calculateDir(mappingProb, vertProb, horiProb));
        } //else if (POS == max && getIsLevy() && myGradient.GetPix(x, y) == 0) {
        else if (getIsLevy() && myGradient.Get(x,y) == 0) {
            if (getStart() == true && getCount() >= 0) // if the cell is just starting its levy walk
            {
                Random rand = new Random();
                vertProb = rand.nextDouble();
                horiProb = rand.nextDouble();
                if (Math.random() < 0.5) {
                    vertProb = -vertProb;
                }
                if (Math.random() < 0.5) {
                    horiProb = -horiProb;
                }
                setVertProb(vertProb);
                setHoriProb(horiProb);
                setDirection(G().calculateDir(1, getVertProb(), getHoriProb())); // pick a random direction
                setCount(getCount() - G().timeStep); // decrement the
                // count
                setStart(false); // cell is no longer just
                // starting
            } else if (getCount() >= 0) // if the cell is
            // currently on its
            // levy walk
            {
                setCount(getCount() - G().timeStep); // decrement the
                // count
                setDirection(G().calculateDir(1, getVertProb(), getHoriProb()));
            } else {
                setIsLevy(false); // cell is no longer on
                // its levy walk
            }
        } else// if the cell is not on a Levy walk or a chemokine gradient
        {
            if (Math.random() < G().levyProb) // probability that the cell starts its
            // levy walk
            {
                setIsLevy(true); // cell is on a levy walk
                setStart(true); // cell starts its levy walk
                setCount((int) (Math.random() * 5 + 5)); // length of levy walk
                // varies from 5 - 9
            } else {
                setDirection(G().randomDir());
            }
        }
        if(getDirection()==POS){
            return false;
        }
        return true;
    }
    void draw(){
        GuiGridVis visAllCells=G().visCells;
        GuiGridVis visMyType=getMyVis();
        visAllCells.SetPix(Xsq(), Ysq(), CellColors[getType()]);
        int activationType=getActivationType();
        if(activationType!=-1) {
            visMyType.SetPix(Xsq(), Ysq(), ActivationColors[getActivationType()]);
        }
        else {
            visMyType.SetPix(Xsq(), Ysq(), CellColors[getType()]);
        }
    }

    void drawMove(int prevX,int prevY){
        GuiGridVis visAllCells=G().visCells;
        GuiGridVis visMyType=getMyVis();
        visAllCells.SetPix(prevX, prevY, COLOR_BLACK);
        visMyType.SetPix(prevX, prevY, COLOR_BLACK);
        draw();
    }
    void Die(){
        GuiGridVis visAllCells=G().visCells;
        GuiGridVis visMyType=getMyVis();
        visAllCells.SetPix(Xsq(), Ysq(), COLOR_BLACK);
        visMyType.SetPix(Xsq(), Ysq(), COLOR_BLACK);
        Dispose();
    }
    void Init(int cellType, int cellDirection, boolean isLevy, int count, boolean start, int tumorCellsKilled, int numDivis, int tdCount, int killTime, boolean tStart, double curr, int tKillTime, int prolif){
        this.cellType=cellType;
        this.cellDirection = cellDirection;
        this.isLevy = isLevy;
        this.count = count;
        this.start = start;
        this.TCK = tumorCellsKilled;
        this.numDivis = numDivis;
        this.tdCount = tdCount;
        this.killTime = killTime;
        this.tStart = tStart;
        this.curr = curr;
        this.tKillTime = tKillTime;
        this.prolif = prolif;
    }
    void Step(){
        switch(cellType){
            case CellTypes.FIBROBLASTi:
//                this.size = 10;
                this.size = 40;
                this.TCK = 0;
                break;
            case TCELLi:
                this.size = 10;
                this.TCK = 0;
                break;
            case TCELLpi:
                this.size = 10;
                this.TCK = 0;
                break;
            case DCELLi:
                this.size = 10;
                this.TCK = 0;
                break;
            case FIBROBLASTAPCM:
//                this.size = 10;
                this.size = 40;
                this.TCK = 0;
                break;
            case TCELLa:
                this.size = 10;
                this.TCK = 0;
                break;
            case DCELLf:
                this.size = 10;
                this.TCK = 0;
                break;
            case DCELLm:
                this.size = 10;
                this.TCK = 0;
            case TUMORCELL:
                this.size = 40;
                this.TCK = 0;
            case BCELLi:
                this.size = 10;
                this.TCK = 0;
            case BCELLa:
                this.size = 10;
                this.TCK = 0;
            case FIBROBLASTT:
                this.size = 10;
                this.TCK = 0;
                break;
            case FIBROBLASTAPCOFF:
                this.size = 10;
                this.TCK = 0;
                break;
            case FIBROBLASTAPCF:
//                this.size = 10;
                this.size = 40;
                this.TCK = 0;
                break;
            case FIBROBLASTB:
                this.size = 10;
                this.TCK = 0;
                break;
            default:
                System.err.println("wrong cell type argument");
        }
        switch(cellDirection){
            case UP:
                break;
            case DOWN:
                break;
            case LEFT:
                break;
            case RIGHT:
                break;
            case POS:
                break;
            default:
                System.err.println("wrong cell direction argument");
        }
    }
    int getType()
    {
        return this.cellType;
    }
    GuiGridVis getMyVis(){
        switch (getType()){
            case TCELLa:
                return G().visT;
            case TCELLi:
                return G().visT;
            case TCELLpi:
                return G().visT;
            case BCELLa:
                return G().visB;
            case BCELLi:
                return G().visB;
            case DCELLi:
                return G().visD;
            case DCELLf:
                return G().visD;
            case DCELLm:
                return G().visD;
            default:
                return G().visFRCTumor;

        }
    }
    int getActivationType(){
        switch (getType()) {
            case TCELLi:
                return 0;
            case DCELLi:
                return 0;
            case BCELLi:
                return 0;
            case TCELLa:
                return 1;
            case DCELLf:
                return 1;
            case BCELLa:
                return 1;
            case DCELLm:
                return 2;
            case TCELLpi:
                return 2;
            default:
                return -1;
        }
    }

    void setType(int cellType)
    {
        this.cellType = cellType;
    }

    int getDirection()
    {
        return this.cellDirection;
    }

    void setDirection(int direction)
    {
        this.cellDirection = direction;
    }

    boolean getIsLevy()
    {
        return this.isLevy;//tbd
    }

    void setIsLevy(boolean tf)
    {
        this.isLevy = tf;
    }

    int getCount()
    {
        return this.count;
    }

    void setCount(int c)
    {
        this.count = c;
    }

    boolean getStart()
    {
        return this.start;
    }

    void setStart(boolean s)
    {
        this.start = s;
    }

    int getTDCount()
    {
        return this.tdCount;
    }

    void setTDCount(int t)
    {
        this.tdCount = t;
    }

    boolean isClose(Cells a, int cellDist)
    {
        return Math.sqrt( Math.pow( a.Xpt() - this.Xpt(), 2 ) + Math.pow( a.Ypt() - this.Ypt(), 2 ) ) <= cellDist; // temp
    }

    int getTCK()
    {
        return this.TCK;
    }

    void setTCK(int TCK)
    {
        this.TCK = TCK;
    }

    void setSizeSpeed()
    {
        if (this.getType() == FIBROBLASTi)
        {
            this.speed = 0*G().timeStep;
//            this.size = 4;
            this.size = 40;

        }
        else if (this.getType() == FIBROBLASTAPCM)
        {
            this.speed = 0*G().timeStep;
//            this.size = 4;
            this.size = 40;

        }
        else if (this.getType() == FIBROBLASTT)
        {
            this.speed = 0*G().timeStep;
            this.size = 4;
        }
        else if (this.getType() == FIBROBLASTAPCOFF)
        {
            this.speed = 0*G().timeStep;
            this.size = 4;
        }
        else if (this.getType() == FIBROBLASTAPCF)
        {
            this.speed = 0*G().timeStep;
//            this.size = 4;
            this.size = 40;
        }
        else if (this.getType() == FIBROBLASTB)
        {
            this.speed = 0*G().timeStep;
            this.size = 4;
        }
        else if (this.getType() == TCELLi)
        {
            this.speed = 2*G().timeStep; // about 10 microns per minute
            this.size = 10;
        }
        else if (this.getType() == TCELLpi)
        {
            this.speed = 2*G().timeStep; // about 10 microns per minute
            this.size = 10;
        }
        else if (this.getType() == TCELLa)
        {
//            if (getTStart() == true)
//            {
//                this.speed = 0*G().timeStep; // stops while it's attacking the tumor cell
//            }
//            else {
//                this.speed = 2 * G().timeStep; // about 10 microns per minute
//            }
this.speed = 2 * G().timeStep; // about 10 microns per minute
            this.size = 10;
        }
        else if (this.getType() == DCELLi)
        {
            this.speed = 1*G().timeStep; // about 5 microns per minute
            this.size = 10;
        }
        else if (this.getType() == DCELLf)
        {
            this.speed = 1*G().timeStep; // about 5 microns per minute
            this.size = 10;
        }
        else if (this.getType() == DCELLm)
        {
            this.speed = 1*G().timeStep; // about 5 microns per minute
            this.size = 10;
        }
        else if (this.getType() == TUMORCELL)
        {
            this.speed = 0*G().timeStep;
            this.size = 40;
        }
        else if (this.getType() == BCELLi)
        {
            this.speed = 1*G().timeStep; // about 5 microns per minute
            this.size = 10;
        }
        else if (this.getType() == BCELLa)
        {
            this.speed = 1*G().timeStep; // about 5 microns per minute
            this.size = 10;
        }
    }

    int getKillTime()
    {
        return this.killTime;
    }

    void setKillTime(int TKT)
    {
        this.killTime = TKT;
    }

    int getSpeed()
    {
        return this.speed;
    }

    void setSpeed (int s)
    {
        this.speed = s;
    }

    boolean getTStart()
    {
        return this.tStart;
    }

    void setTStart(boolean tStart)
    {
        this.tStart = tStart;
    }

    int getNumDivis()
    {
        return this.numDivis;
    }

    void setNumDivis(int n)
    {
        this.numDivis = n;
    }

    void setSize(int s)
    {
        this.size = s;
    }

    int getSize()
    {
        return this.size;
    }

    void setVertProb(double v)
    {
        this.vertProb = v;
    }

    void setHoriProb(double h)
    {
        this.horiProb = h;
    }

    double getVertProb()
    {
        return this.vertProb;
    }

    double getHoriProb()
    {
        return this.horiProb;
    }

    int getTKillTime()
    {
        return this.tKillTime;
    }

    void setTKillTime(int tKillTime)
    {
        this.tKillTime = tKillTime;
    }

    int getProlif()
    {
        return this.prolif;
    }

    void setProlif(int prolif)
    {
        this.prolif = prolif;
    }

    void activate(Cells a)
    {
        if (this.getType() == DCELLf && a.getType() == FIBROBLASTi) // activates the FRC to FRCAPCF if it contacts an APCF
        {
            a.activateFRCF();
        }
        else if (this.getType() == FIBROBLASTi && a.getType() == DCELLf)  // activates the FRC to FRCAPCF if it contacts an APCF
        {
            this.activateFRCF();
        }
        else if (this.getType() == DCELLm && a.getType() == FIBROBLASTi) // activates the FRC to FRCAPCM if it contacts an APCM
        {
            a.activateFRCM();
        }
        else if (this.getType() == FIBROBLASTi && a.getType() == DCELLm)  // activates the FRC to FRCAPCM if it contacts an APCM
        {
            this.activateFRCM();
        }
        else if (this.getType() == DCELLm && a.getType() == TCELLi) // activates the T-cell if it contacts an active D-cell
        {
            a.activate();
        }
        else if (this.getType() == TCELLi && a.getType() == DCELLm) // activates the T-cell if it contacts an active D-cell
        {
            this.activate();
        }
        else if (this.getType() == DCELLi && a.getType() == TUMORCELL) // activates the D-cell if it contacts a tumor cell
        {
            this.activateDCM();
        }
        else if (this.getType() == TUMORCELL && a.getType() == DCELLi) // activates the D-cell if it contacts a tumor cell
        {
            a.activateDCM();
        }
        else if (this.getType() == DCELLf && a.getType() == BCELLi) // activates the B-cell if it contacts an active D-cell
        {
            a.activate();
        }
        else if (this.getType() == BCELLi && a.getType() == DCELLf) // activates the B-cell if it contacts an active D-cell
        {
            this.activate();
        }
        else if (this.getType() == DCELLm && a.getType() == BCELLi) // activates the B-cell if it contacts an active D-cell
        {
            a.activate();
        }
        else if (this.getType() == BCELLi && a.getType() == DCELLm) // activates the B-cell if it contacts an active D-cell
        {
            this.activate();
        }
        else if (this.getType() == DCELLi && a.getType() == BCELLa) // activates the APC to APCF if it contacts a B cell
        {
            this.activateDCF();
        }
        else if (this.getType() == BCELLa && a.getType() == DCELLi) // activates the APC to APCF if it contacts a B cell
        {
            a.activateDCF();
        }
        else if (this.getType() == DCELLi && a.getType() == TCELLa) // activates the APC to APCM if it contacts a T cell
        {
            this.activateDCM();
        }
        else if (this.getType() == TCELLa && a.getType() == DCELLi) // activates the APC to APCM if it contacts a T cell
        {
            a.activateDCM();
        }
    }
    void activate() // activates the cell
    {
        if (this.cellType == TCELLi)
        {
            this.cellType = TCELLa;
            this.setTCK( (int)(Math.random() * 8) + 3 ); // between 7 and 10 kills per T-cell
            //this.setTCK(5);
            this.numDivis = (int)(Math.random() * 3) + 3; // proliferates between 3 and 5 times per T-cell activation
//            this.tdCount = (int) (Math.random() * 16) + 30; // T-cell stops for 2-3 hours at the FRC patch
            this.tdCount = (int) (Math.random() * 16) + 165; // T-cell stops for 11-12 hours at the FRC patch
            this.killTime = G().getCellLifeSpan(G().timeStep);
            this.prolif = G().getProlif(G().timeStep);
        }
        else if (this.cellType == BCELLi)
        {
            this.cellType = BCELLa;
            this.numDivis = 0;
            this.killTime = G().getCellLifeSpan(G().timeStep);
            this.prolif = G().getProlif(G().timeStep);
        }
    }

    void activateDCM()
    {
        this.cellType = DCELLm;
    }
    void activateDCF()
    {
        this.cellType = DCELLf;
    }
    void activateFRCM()
    {
        this.cellType = FIBROBLASTAPCM;
        tdCount = 240; // 16 hours
        this.numDivis = 0;
    }
    void activateFRCF()
    {
        this.cellType = FIBROBLASTAPCF;
        tdCount = 240; // 16 hours
        this.numDivis = 0;
    }

    void setCurr(double curr)
    {
        this.curr = curr;
    }

    double getCurr()
    {
        return this.curr;
    }

}