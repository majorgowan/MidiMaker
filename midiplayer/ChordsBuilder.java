package midiplayer;

// Swing packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// For resizable arrays
import java.util.ArrayList;

class ChordsBuilder extends JPanel implements ActionListener {

    private JButton addChordButton = new JButton("Add chord");
    private JButton applyButton = new JButton("Apply");
    private ArrayList<Chord> localChords;
    private ArrayList<JButton> removeButtons;
    private ArrayList<ChordLine> chordLines;

    public void actionPerformed(ActionEvent ae) {
        // listen for remove buttons, addbutton and applybutton
        // do the necessary
    }

    public static void resetChordsBuilderPanel() {

    }

    // constructor
    ChordsBuilder(ArrayList<Chord> chords) {

        removeButtons = new ArrayList<JButton>(0);
        chordLines = new ArrayList<ChordLine>(0);

        // make copy of the chords object in case user
        // chickens out
        localChords = new ArrayList<Chord>(chords);

        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
        
        for (int i=0; i < localChords.size(); i++) {
            System.out.println(localChords.get(i).getName());
            removeButtons.add(new JButton("remove"));
            chordLines.add(new ChordLine(localChords.get(i),
                        removeButtons.get(i),i+1));
            removeButtons.get(i).setActionCommand("rem"+i);
            mainPanel.add(chordLines.get(i));
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);

        buttonPanel.add(addChordButton);
        buttonPanel.add(applyButton);
        buttonPanel.setPreferredSize(new Dimension(120,100));

        this.add(buttonPanel);
        this.add(scrollPane);

    }
}



