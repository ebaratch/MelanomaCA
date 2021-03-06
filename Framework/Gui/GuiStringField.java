package Framework.Gui;

import Framework.Interfaces.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * a menu item that takes string input
 */
public class GuiStringField extends JTextField implements Framework.Interfaces.MenuItem,GuiComp {
    String labelText;
    public JLabel label;
    int compX;
    int compY;
    int nCols;
    private final String initVal;

    /**
     * @param label the label of the menuString
     * @param initVal the starting value of the menuString
     */
    public GuiStringField(String label, String initVal){
        super(10);
        this.initVal=initVal;
        nCols=10;
        this.compX=1;
        this.compY=2;
        this.labelText=label;
        this.label=new JLabel(labelText);
    }

    /**
     * @param label the label of the menuString
     * @param initVal the starting value of the menuString
     * @param nCols the number of characters that will fit on the display
     * @param compX the width on the gui GridBagLayout
     * @param compY the height on the gui GridBagLayout
     */
    public GuiStringField(String label, String initVal, int nCols, int compX, int compY){
        super(nCols);
        this.initVal=initVal;
        this.nCols=nCols;
        this.compX=compX;
        this.compY=compY;
        this.labelText=label;
        this.label=new JLabel(labelText);
    }

    /**
     * sets the foreground and background of the GuiStringField
     * @param foregroundColor color of the text if null the GuiWindow color will be used
     * @param backgroundColor color of the background, if null the GuiWindow color will be used
     */
    public GuiStringField SetColor(Color foregroundColor, Color backgroundColor){
        if(backgroundColor!=null){
            setOpaque(true);
            setBackground(backgroundColor);
            label.setOpaque(true);
            label.setBackground(backgroundColor);
        }
        if(foregroundColor !=null) {
            setForeground(foregroundColor);
            label.setForeground(foregroundColor);
            setCaretColor(foregroundColor);
        }
        return this;
    }

    public GuiStringField SetColor(int foregroundColor, int backgroundColor){
        SetColor(new Color(foregroundColor),new Color(backgroundColor));
        return this;
    }
    /**
     * ignore
     */
    @Override
    public int TypeID() {
        return 3;
    }

    /**
     * ignore
     */
    @Override
    public void Set(String val) {
        if(val.length()>nCols){ val=val.substring(0,nCols); }
        this.setText(val);
    }

    /**
     * ignore
     */
    @Override
    public String Get() {
        return this.getText();
    }

    /**
     * ignore
     */
    @Override
    public String GetLabel() {
        return labelText;
    }

    /**
     * ignore
     */
    @Override
    public int NEntries() {
        return 2;
    }


    /**
     * ignore
     */
    @Override
    public <T extends Component> T GetEntry(int iEntry) {
        switch(iEntry){
            case 0: return (T)label;
            case 1: return (T)this;
            default: throw new IllegalArgumentException(iEntry+" does not match to an item!");
        }
    }

    @Override
    public String _GetInitValue() {
        return initVal;
    }


    /**
     * ignore
     */
    @Override
    public int compX() {
        return compX;
    }

    /**
     * ignore
     */
    @Override
    public int compY() {
        return compY;
    }

    @Override
    public boolean IsActive() {
        return true;
    }

    @Override
    public void SetActive(boolean isActive) { }

    /**
     * ignore
     */
    @Override
    public void GetComps(ArrayList<Component> putHere, ArrayList<Integer> coordsHere, ArrayList<Integer> compSizesHere) {
        int labelEnd=compY/2;
        putHere.add(this.label);
        coordsHere.add(0);
        coordsHere.add(0);
        compSizesHere.add(compX);
        compSizesHere.add(labelEnd);
        putHere.add(this);
        coordsHere.add(0);
        coordsHere.add(labelEnd);
        compSizesHere.add(compX);
        compSizesHere.add(compY-labelEnd);
    }
}
