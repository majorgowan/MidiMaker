package midiplayer;

// Swing packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// For resizable arrays
import java.util.ArrayList;

class ChordsBuilder extends Panel implements ActionListener {

    private JPanel mainPanel;
    private JButton addChordButton = new JButton("Add chord");
    private JButton applyButton = new JButton("Apply");
    private ArrayList<Chord> localChords;
    private ArrayList<JButton> removeButtons;
    private ArrayList<ChordLine> chordLines;

    public void actionPerformed(ActionEvent ae) {
        // listen for remove buttons, addbutton and applybutton
        // do the necessary

        // find out who detected the event
        String comStr = ae.getActionCommand();

        if (comStr.equals("Add chord")) {

            System.out.println("Adding a chord yo!");

            Chord newChord = new Chord();
            JButton newRemove = new JButton("remove");

            localChords.add(newChord);
            removeButtons.add(newRemove);
            chordLines.add(new ChordLine(newChord, newRemove, chordLines.size()+1));

            mainPanel.add(chordLines.get(chordLines.size()-1));

            SwingUtilities.windowForComponent(this).pack();

            System.out.println();
            for (int i=0; i<localChords.size(); i++)
                System.out.println(localChords.get(i).getName());

        } else if (comStr.equals("Apply")) {
            System.out.println("Apply changes!");
            for (int i = 0; i < chordLines.size(); i++) {
                System.out.println("Setting chord # " + i);
                chordLines.get(i).setChord();
            }
        }

    }

    // constructor
    ChordsBuilder(ArrayList<Chord> chords) {

        removeButtons = new ArrayList<JButton>(0);
        chordLines = new ArrayList<ChordLine>(0);

        // make copy of the chords object in case user
        // chickens out
        //localChords = new ArrayList<Chord>(chords);

        localChords = chords;

        mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
        
        for (int i=0; i < localChords.size(); i++) {
            removeButtons.add(new JButton("remove"));
            chordLines.add(new ChordLine(localChords.get(i),
                        removeButtons.get(i),i+1));
            removeButtons.get(i).setActionCommand("rem"+i);
            mainPanel.add(chordLines.get(i));

            removeButtons.get(i).addActionListener(this);
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);

        buttonPanel.add(addChordButton);
        addChordButton.addActionListener(this);

        buttonPanel.add(applyButton);
        applyButton.addActionListener(this);

        buttonPanel.setPreferredSize(new Dimension(120,100));

        this.add(buttonPanel);
        this.add(scrollPane);

    }
}



