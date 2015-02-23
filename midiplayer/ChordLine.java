package midiplayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class ChordLine extends JPanel implements ItemListener {

    private Chord chord;
    private JCheckBox[] checkbox = new JCheckBox[17];
    private JLabel numberLabel;
    private JTextField nameField;
    private JTextField symbolField;

    public void itemStateChanged(ItemEvent ie) {
        // each time a checkbox is changed, scan the set
        // of checkboxes.  If six are selected, disable
        // the unselected boxes.  If fewer than six are
        // selected, enable all boxes.
        
        int count = 0;
        for (int i = 0; i < checkbox.length; i++)
            if (checkbox[i].isSelected())
                count++;

        if (count >= 6) {
            for (int i = 0; i < checkbox.length; i++) {
                if (!(checkbox[i].isSelected()))
                    checkbox[i].setEnabled(false);
            }
        } else {
            for (int i = 0; i < checkbox.length; i++)
                checkbox[i].setEnabled(true);
        }
        
    }

    public void setChord() {

        int j = 0;
        int[] notes = new int[6];
        for (int i = 0; i < checkbox.length; i++)
            if (checkbox[i].isSelected())
                notes[j++] = i;
        for (int i = j; i < notes.length; i++)
            notes[i] = 100;

        chord.reInit(nameField.getText(), symbolField.getText(), notes);

    }

    ChordLine() {
        this.setLayout(new FlowLayout());
    }

    ChordLine(Chord c, JButton remButton, int number) {
        this();

        chord = c;

        numberLabel = new JLabel(""+number,2);
        nameField = new JTextField(chord.getName(),10);
        symbolField = new JTextField(chord.getSymbol(),2);

        for (int i=0; i<checkbox.length; i++) {
            checkbox[i] = new JCheckBox(""+i);
            checkbox[i].setHorizontalTextPosition(SwingConstants.CENTER);
            checkbox[i].setVerticalTextPosition(SwingConstants.TOP);
            checkbox[i].addItemListener(this);
        }

        int[] notes = chord.getNotes();
        for (int j=0; j<notes.length; j++)
            if (notes[j] != 100) 
                checkbox[notes[j]].setSelected(true);

        this.add(numberLabel);
        this.add(nameField);
        this.add(symbolField);

        for (int i=0; i<checkbox.length; i++)
            this.add(checkbox[i]);

        this.add(remButton);
    }
}





