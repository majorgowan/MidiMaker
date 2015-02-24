package midiplayer;

// Swing packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// For resizable arrays
import java.util.ArrayList;

class ChordsBuilder extends JPanel implements ActionListener {

    private MidiPlayer midiPlayer;
    private JPanel mainPanel;
    private JButton addChordButton = new JButton("Add chord");
    private JButton okButton = new JButton("Ok");
    private JButton cancelButton = new JButton("Cancel");
    private ArrayList<Chord> localChords;
    private ArrayList<JButton> removeButtons;
    private ArrayList<ChordLine> chordLines;

    public void actionPerformed(ActionEvent ae) {
        // listen for remove buttons, addbutton and okbutton
        // do the necessary

        // find out who detected the event
        String comStr = ae.getActionCommand();

        if (comStr.equals("Add chord")) {

            Chord newChord = new Chord();
            JButton newRemove = new JButton("remove");

            localChords.add(newChord);
            removeButtons.add(newRemove);
            chordLines.add(new ChordLine(newChord, newRemove, chordLines.size()+1));

            newRemove.addActionListener(this);
            newRemove.setActionCommand("rem"+(chordLines.size()-1));

            mainPanel.add(chordLines.get(chordLines.size()-1));

            SwingUtilities.windowForComponent(this).pack();

            System.out.println();

        } else if (comStr.equals("Ok")) {
            for (int i = 0; i < chordLines.size(); i++) {
                chordLines.get(i).setChord();
            }

            midiPlayer.setChords(localChords);
            midiPlayer.refreshChordsPanel();

            SwingUtilities.windowForComponent(this).setVisible(false);
            SwingUtilities.windowForComponent(this).dispose();
        } else if (comStr.equals("Cancel")) {
            SwingUtilities.windowForComponent(this).setVisible(false);
            SwingUtilities.windowForComponent(this).dispose();
        }

        for (int i = 0; i < localChords.size(); i++) {
            if (comStr.equals("rem"+i)) {
                mainPanel.remove(chordLines.get(i));
                localChords.remove(i);
                chordLines.remove(i);
                this.revalidate();
            }
        }
    }

    // constructor
    ChordsBuilder(MidiPlayer midi, ArrayList<Chord> chords) {

        midiPlayer = midi;

        removeButtons = new ArrayList<JButton>(0);
        chordLines = new ArrayList<ChordLine>(0);

        // make copy of the chords object in case user
        // chickens out
        localChords = new ArrayList<Chord>(chords);

        mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.PAGE_AXIS));

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
        addChordButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(Box.createRigidArea(new Dimension(0,25)));

        buttonPanel.add(okButton);
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(Box.createRigidArea(new Dimension(0,10)));

        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(buttonPanel);
        this.add(scrollPane);
            
    }
}



