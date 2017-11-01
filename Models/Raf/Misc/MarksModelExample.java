package Models.Raf.Misc;

import Framework.Extensions.MarksModelCell;
import Framework.Extensions.MarksModelGrid;
import Framework.Gui.GuiGridVis;
import Framework.Gui.GuiLabel;
import Framework.Gui.GuiWindow;
import Framework.Gui.TickTimer;

/**
 * Created by bravorr on 6/28/17.
 */

class MarksModelTestGrid extends MarksModelGrid<MarksModelTestCell>{

    public MarksModelTestGrid(int x, int y) {
        super(x, y, MarksModelTestCell.class);
    }
    public void Step(){
    }
}

class MarksModelTestCell extends MarksModelCell<MarksModelTestCell,MarksModelTestGrid>{

}

public class MarksModelExample {
    public static void main(String[] args) {
        int x=200;
        int y=200;
        boolean visOn=true;
        GuiWindow win=new GuiWindow("testDisp",true,visOn);
        GuiGridVis visCells=new GuiGridVis(x,y,2,visOn);
        GuiGridVis visO2=new GuiGridVis(x,y,2,visOn);
        GuiGridVis visAcid=new GuiGridVis(x,y,2,visOn);
        GuiGridVis visGlu=new GuiGridVis(x,y,2,visOn);
        GuiGridVis visPheno=new GuiGridVis(x,y,4,3,3, visOn);
        win.AddCol(0, new GuiLabel("Cells",visOn));
        win.AddCol(0, visCells);
        win.AddCol(0, new GuiLabel("Oxygen",visOn));
        win.AddCol(0, visO2);
        win.AddCol(1, new GuiLabel("pH",visOn));
        win.AddCol(1, visAcid);
        win.AddCol(1, new GuiLabel("Glucose",visOn));
        win.AddCol(1, visGlu);
        win.AddCol(2, new GuiLabel("red: acid resist green: glycolytic",visOn));
        win.AddCol(2, visPheno);
        win.RunGui();
        MarksModelTestGrid a=new MarksModelTestGrid(x,y);
        a.FillGrid(0.8);
        a.CreateTumor(5,a.xDim/2,a.yDim/2);
        a.InitDiffusibles();
        TickTimer trt=new TickTimer();
        for (int i = 0; i < 10000; i++) {
            trt.TickPause(0);
            a.DefaultStep();
            a.DrawCells(visCells);
            a.DrawMicroEnvHeat(visO2,false,false,true);
            a.DrawMicroEnvHeat(visAcid,false,true,false);
            a.DrawMicroEnvHeat(visGlu,true,false,false);
            a.DrawPheno(visPheno);
        }
        win.Dispose();
    }
}
