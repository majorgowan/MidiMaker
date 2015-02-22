// Swing packages
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

// For reading and writing files
import java.io.*;
import javax.imageio.*;

// For resizable arrays
import java.util.ArrayList;

// For printing arrays to screen
import java.util.Arrays;

// JMusic packages
import jm.JMC;
import jm.music.data.*;
import jm.util.*;

// Class for chord objects
class Chord {

    private String name;
    private String symbol;
    private int[] notes = new int[6];

    Chord(String nm, String sym, int[] no) {
        name = nm;
        symbol = sym;
        for (int i = 0; i < no.length; i++)
            notes[i] = no[i];
        for (int i = no.length; i < 6; i++)
            notes[i] = 100;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int[] getNotes() {
        return notes;
    }

    public void reInit(String nm, String sym, int[] no) {
        name = nm;
        symbol = sym;
        for (int i = 0; i < no.length; i++)
            notes[i] = no[i];
        for (int i = no.length; i < 6; i++)
            notes[i] = 100;
    }
}

// class for a musical phrase
final class Piece implements JMC {

    Phrase phrase;

    Piece(int tempo) {
        phrase = new Phrase(2.0);
        phrase.setTempo((double)tempo);
    }

    public void setTempo(int tempo) {
        phrase.setTempo((double)tempo);
    }

    public void reset() {
        phrase.empty();
    }

    // add a note or a rest to the phrase
    public void addThisNote(int n) {
        if (n == 100) {
            phrase.addNote(new Rest());
        } else {
            phrase.addNote(new Note(C4+n, C));
        }
    }

    // get phrase (to add to a score)
    public Phrase getPhrase() {
        return phrase;
    }
}

// class for a chord progression
final class Prog implements JMC {

    // Chord progression represented by (up to) 6 overlapping voices
    Phrase[] cphrase = new Phrase[6];

    Prog(int tempo) {
        for (int i=0; i<cphrase.length; i++) {
            cphrase[i] = new Phrase(2.0);
            cphrase[i].setTempo((double)tempo);
        }
    }

    public void setTempo(int tempo) {
        for (int i=0; i<cphrase.length; i++) {
            cphrase[i].setTempo((double)tempo);
        }
    }

    public void reset() {
        for (int i=0; i<cphrase.length; i++)
            cphrase[i].empty();
    }

    // add a chord to the phrase (position = beat in measure)
    public void addThisChord(int n, Chord chord, int position) {
        int dynamic;
        if (position == 0) 
            dynamic = JMC.MF;
        else 
            dynamic = JMC.P; 

        if (n == 100) {
            for (int i = 0; i < cphrase.length; i++) 
                cphrase[i].addNote(new Rest());
        } else {
            for (int i = 0; i < cphrase.length; i++) {
                int[] notes = chord.getNotes();
                if (notes[i] == 100) 
                    cphrase[i].addNote(new Rest());
                else
                    cphrase[i].addNote(new Note(C4+n+chord.getNotes()[i], C, dynamic));
            }
        }
    }

    // get chord phrase (to add to a score)
    public Phrase getPhrase(int i) {
        return cphrase[i];
    }
}

// class for drawing staffs and notes and listening for
// mouse activity
class Staff extends JPanel {

    // beats per measure and number of measures
    private int beatsPer, measures;

    // arrays for note values and qualities (natural, sharp or flat)
    private int[] notes;
    private int[] noteTypes;

    // JMusic component for this staff
    private Piece piece;

    // Geometry
    private int top, left, leftplus, halfspace, width;

    // Trebble clef
    private BufferedImage tc_img;

    // C Major scale for converting musical notation to
    // pitch values
    private final int[] CMajor = {0, 2, 4, 5, 7, 9, 11};

    Staff() {

        setBackground(Color.WHITE);

        tc_img = null;
        try {
            tc_img = ImageIO.read(new File("treble.png"));
        } catch (IOException e) {
            System.out.println("Treble clef image not found!");
        }

        // listener for interpreting mouse clicks
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if ((me.getX() > leftplus) && (me.getX() < (leftplus+width))) { 
                    if (SwingUtilities.isLeftMouseButton(me)) {
                        if (me.isShiftDown()) {
                            addNote(me.getX(),me.getY(),-1);
                        } else if (me.isControlDown()) {
                            addNote(me.getX(),me.getY(),1);
                        } else {
                            addNote(me.getX(),me.getY(),0);
                        }
                    }
                    else if (SwingUtilities.isRightMouseButton(me)) {
                        addRest(me.getX());
                    }
                }
            } 
        });
    }

    // set parameters for this voice
    // (not in constructor so that it can be set
    //  based on user input or loading from file)
    public void setParams(int bP, int m, Piece p) {
        beatsPer = bP;
        measures = m;
        piece = p;

        notes = new int[measures*beatsPer];
        noteTypes = new int[measures*beatsPer];

        // fill voice with "rests"
        for (int i = 0; i<notes.length; i++) {
            notes[i] = 100;
            noteTypes[i] = 0;
        }

        // geometry of staff
        top = 50;
        halfspace = 4;
        left = 20;
        leftplus = left + 7*halfspace;
        width = beatsPer*measures*4*halfspace+2;

        repaint();
    }

    // add a note to the staff
    private void addNote(int mouseX, int mouseY, int noteType) {
        // locate in time:
        int whichNote = (mouseX-leftplus-2)/(4*halfspace);
        // locate in pitch:
        int intervals = (mouseY - (top+halfspace/2)) / halfspace;
        noteTypes[whichNote] = noteType; 
        notes[whichNote] = intervals;
        repaint();
    }

    // add (restore) a rest to the staff 
    private void addRest(int mouseX) {
        // locate in time:
        int whichNote = (mouseX-leftplus-2)/(4*halfspace);
        notes[whichNote] = 100;
        repaint();
    }

    // convert musical notation to series of notes
    public void build(int playStart, int playEnd) {
        piece.reset();

        // crop dead tail
        int length = 0;
        for (int i = 0; i < notes.length; i++) {
            if (notes[i] != 100)
                length = i+1;
        }

        // notes stored in diatonic steps from E5 (top gap
        // of treble clef).  First convert note to distance from
        // nearest C; then calculate octave above middle C; finally
        // add appropriate note to the JMusic object
        for (int i = playStart; i < Math.min(length,playEnd); i++) {
            int notesFromC = -(notes[i] - 2);
            int whichOctave = (int)Math.floor((double)(notesFromC)/7);
            if (notes[i] == 100)
                piece.addThisNote(100);
            else
                piece.addThisNote(12+12*whichOctave + noteTypes[i]
                        + CMajor[notesFromC-whichOctave*7]);
        }
    }

    // reset this piece to all rests
    public void reset() {
        for (int i = 0; i < notes.length; i++)
            notes[i] = 100;
        piece.reset();
        repaint();
    }

    // return String representation of a given note and type
    // (for writing to MMM file)
    public String getNoteAndType(int index) {
        String s = notes[index] + " " + noteTypes[index];
        return s;
    }

    // Assign a note and type (for reading from MMM file)
    public void setNoteAndType(int index, int note, int type) {
        notes[index] = note;
        noteTypes[index] = type;
    }

    // draw staff and notes!!
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // draw treble clef image
        g.drawImage(tc_img,left-3*halfspace,top-2*halfspace,
                13*halfspace,13*halfspace, Color.WHITE, null);

        for (int i = 0; i < 5; i++) 
            g2.drawLine(left,top+2*i*halfspace,leftplus+width,top+2*i*halfspace);

        for (int i = 1; i <= measures; i++) 
            g2.drawLine(leftplus + 4*halfspace*beatsPer*i, top,
                    leftplus + 4*halfspace*beatsPer*i, top+8*halfspace);

        // add small measure numbers above the staff
        g2.setFont(new Font("SansSerif",Font.ITALIC,10));
        for (int i = 0; i < measures; i++) 
            g2.drawString(""+(i+1),
                    leftplus + 4*halfspace*beatsPer*i+halfspace/2, top-halfspace/2);

        for (int i = 0; i < notes.length; i++) {
            g2.setStroke(new BasicStroke(1));
            g2.setColor(Color.BLACK);
            if (notes[i] == 100) {
                g2.drawRect(leftplus + 4*halfspace*i+2, 
                        top+halfspace*3, 
                        3*halfspace, 
                        2*halfspace);
            } else {
                // add extra lines above or below the staff if necessary
                g2.setStroke(new BasicStroke(1));
                for (int j = 0; j > notes[i]; j-=2) {
                    g2.drawLine(leftplus + 4*halfspace*i,
                            top+halfspace*j,
                            leftplus+4*halfspace*i+3*halfspace,
                            top+halfspace*j);
                } 
                for (int j = 9; j <= notes[i]; j+=2) {
                    g2.drawLine(leftplus + 4*halfspace*i,
                            top+halfspace*(j+1),
                            leftplus+4*halfspace*i+3*halfspace,
                            top+halfspace*(j+1));
                } 
                // draw note (red for sharp, blue for flat)
                if (noteTypes[i] == 1)
                    g2.setColor(Color.RED);
                else if (noteTypes[i] == -1)
                    g2.setColor(Color.BLUE);
                else
                    g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.fillOval(leftplus + 4*halfspace*i+1, 
                        top+halfspace*notes[i], 
                        2*halfspace+halfspace/2, 
                        2*halfspace);
                g2.drawLine(leftplus + 4*halfspace*i+2*halfspace+halfspace/2,
                        top+halfspace*notes[i]+halfspace/2+1,
                        leftplus + 4*halfspace*i+2*halfspace+halfspace/2,
                        top+halfspace*(notes[i])-3*halfspace);
            }
        }
    }
}

// class for drawing a chord progression and listening for
// mouse activity
class ChordStaff extends JPanel {

    // beats per measure and number of measures
    private int beatsPer, measures;

    // arrays for note values and qualities (natural, sharp or flat)
    private int[] notes;
    private int[] noteTypes;
    private int[] chordQualities;
    private ArrayList<Chord> chords;

    // JMusic component for this staff
    private Prog prog;

    // Geometry
    private int top, left, leftplus, halfspace, width;

    // Trebble clef
    private BufferedImage tc_img;

    // C Major scale for converting musical notation to
    // pitch values
    private final int[] CMajor = {0, 2, 4, 5, 7, 9, 11};

    ChordStaff(ArrayList<Chord> cs) {

        chords = cs;

        setBackground(Color.WHITE);

        tc_img = null;
        try {
            tc_img = ImageIO.read(new File("treble.png"));
        } catch (IOException e) {
            System.out.println("Treble clef image not found!");
        }

        // listener for interpreting mouse clicks
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if ((me.getX() > leftplus) && (me.getX() < (leftplus+width))) { 
                    if (SwingUtilities.isLeftMouseButton(me)) {
                        if (me.isShiftDown()) {
                            addChord(me.getX(),me.getY(),-1);
                        } else if (me.isControlDown()) {
                            addChord(me.getX(),me.getY(),1);
                        } else {
                            addChord(me.getX(),me.getY(),0);
                        }
                    }
                    else if (SwingUtilities.isRightMouseButton(me)) {
                        addRest(me.getX());
                    }
                }
            } 
        });
    }

    public void setParams(int bP, int m, Prog p) {
        beatsPer = bP;
        measures = m;
        prog = p;

        notes = new int[measures*beatsPer];
        noteTypes = new int[measures*beatsPer];
        chordQualities = new int[measures*beatsPer];

        // fill voice with "rests"
        for (int i = 0; i<notes.length; i++) {
            notes[i] = 100;
            noteTypes[i] = 0;
            chordQualities[i] = chords.size() - 1;
        }

        // geometry of staff
        top = 50;
        halfspace = 4;
        left = 20;
        leftplus = left + 7*halfspace;
        width = beatsPer*measures*4*halfspace+2;

        repaint();
    }

    // add a note to the staff
    private void addChord(int mouseX, int mouseY, int noteType) {
        // locate in time:
        int whichNote = (mouseX-leftplus-2)/(4*halfspace);
        // locate in pitch:
        int intervals = (mouseY - (top+halfspace/2)) / halfspace;
        noteTypes[whichNote] = noteType; 
        notes[whichNote] = intervals;
        chordQualities[whichNote] 
            = (chordQualities[whichNote] + 1) % chords.size();
        repaint();
    }

    // add (restore) a rest to the staff 
    private void addRest(int mouseX) {
        // locate in time:
        int whichNote = (mouseX-leftplus-2)/(4*halfspace);
        notes[whichNote] = 100;
        chordQualities[whichNote] = chords.size()-1;
        repaint();
    }

    // convert musical notation to series of notes
    public void build(int playStart, int playEnd) {
        prog.reset();

        // crop dead tail
        int length = 0;
        for (int i = 0; i < notes.length; i++) {
            if (notes[i] != 100)
                length = i+1;
        }

        // notes stored in diatonic steps from F5 (top line
        // of treble clef).  First convert note to distance from
        // nearest C; then calculate octave above middle C; finally
        // add appropriate note to the JMusic object
        for (int i = playStart; i < Math.min(length,playEnd); i++) {
            int notesFromC = -(notes[i] - 2);
            int whichOctave = (int)Math.floor((double)(notesFromC)/7);
            if (notes[i] == 100)
                prog.addThisChord(100, chords.get(chords.size()-1), 0);
            else 
                prog.addThisChord(12+12*whichOctave + noteTypes[i]
                        + CMajor[notesFromC-whichOctave*7],
                        chords.get(chordQualities[i]), i % beatsPer);
        }
    }

    // reset this piece to all rests
    public void reset() {
        for (int i = 0; i < notes.length; i++)
            notes[i] = 100;
        prog.reset();
        repaint();
    }

    // return String representation of a given note and type
    // (for writing to MMM file)
    public String getNoteAndTypeAndQuality(int index) {
        String s = notes[index] + " " + noteTypes[index]
            + " " + chordQualities[index];
        return s;
    }

    // Assign a tonic note, type and quality (for reading from MMM file)
    public void setChordAndTypeAndQuality(int index, int note, int type,
            int quality) {
        notes[index] = note;
        noteTypes[index] = type;
        chordQualities[index] = quality;
    }

    // draw staff and notes!!
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        String qstring;

        // draw treble clef image
        g.drawImage(tc_img,left-3*halfspace,top-2*halfspace,
                13*halfspace,13*halfspace, Color.WHITE, null);

        for (int i = 0; i < 5; i++) 
            g2.drawLine(left,top+2*i*halfspace,leftplus+width,top+2*i*halfspace);

        for (int i = 1; i <= measures; i++) 
            g2.drawLine(leftplus + 4*halfspace*beatsPer*i, top,
                    leftplus + 4*halfspace*beatsPer*i, top+8*halfspace);

        // add small measure numbers above the staff
        g2.setFont(new Font("SansSerif",Font.ITALIC,10));
        for (int i = 0; i < measures; i++) 
            g2.drawString(""+(i+1),
                    leftplus + 4*halfspace*beatsPer*i+halfspace/2, top-halfspace/2);

        for (int i = 0; i < notes.length; i++) {
            qstring = chords.get(chordQualities[i]).getSymbol();
            g2.setStroke(new BasicStroke(1));
            g2.setColor(Color.BLACK);
            if (notes[i] == 100) {
                g2.drawRect(leftplus + 4*halfspace*i+2, 
                        top+halfspace*3, 
                        3*halfspace, 
                        2*halfspace);
            } else {
                // add extra lines above or below the staff if necessary
                g2.setStroke(new BasicStroke(1));
                for (int j = 0; j > notes[i]; j-=2) {
                    g2.drawLine(leftplus + 4*halfspace*i,
                            top+halfspace*j,
                            leftplus+4*halfspace*i+3*halfspace,
                            top+halfspace*j);
                } 
                for (int j = 9; j <= notes[i]; j+=2) {
                    g2.drawLine(leftplus + 4*halfspace*i,
                            top+halfspace*(j+1),
                            leftplus+4*halfspace*i+3*halfspace,
                            top+halfspace*(j+1));
                } 
                // print Chord quality (red for sharp, blue for flat)
                if (noteTypes[i] == 1)
                    g2.setColor(Color.RED);
                else if (noteTypes[i] == -1)
                    g2.setColor(Color.BLUE);
                else
                    g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.setFont(new Font("SansSerif",Font.BOLD,3*halfspace+2));
                g2.drawString(qstring,
                        leftplus + 4*halfspace*i, 
                        top+halfspace*notes[i]+2*halfspace+1); 
            }
        }
    }
}

// play score in independent thread (big improvement!)
class PlayScore implements Runnable {
    String thrdName;
    private Score score;

    PlayScore(String name, Score s) {
        thrdName = name;
        score = s;
    }

    public void run() {
        System.out.println(thrdName + " starting.");
        Play.midi(score);
        System.out.println(thrdName + " terminating!");
    }
}


// main class:
public class MidiPlayer implements ActionListener {

    // some components
    private JFrame jfrm;
    private JPanel mainPanel, staffPanel;
    private JScrollPane scrollPane;
    private ArrayList<JPanel> staffLine;
    private ArrayList<JCheckBox> checkBox;

    // musical stuff
    private int nvoices, nchordVoices;
    private ArrayList<Piece> voice;
    private ArrayList<Prog> chordVoice;
    private ArrayList<Staff> staff;
    private ArrayList<ChordStaff> chordStaff;
    private ArrayList<Chord> chords;
    private Score allTogether = new Score();
    private int beatsPer, measures, tempo;

    // play interval
    private int playStart, playEnd;

    // clear all ArrayLists (before making new piece or loading piece)
    private void clearLists() {
        staffLine.clear();
        checkBox.clear();
        voice.clear();
        chordVoice.clear();
        staff.clear();
        chordStaff.clear();
        chords.clear();
    }

    private void initSwingLists() {
        sizeStaffPanels();

        // JPanel for each voice and a checkBox to turn each on or off
        staffLine.clear();
        checkBox.clear();
        for (int j=0; j<nvoices; j++) {
            checkBox.add(new JCheckBox("Voice " + (j+1) + "  ", true));
            staffLine.add(new JPanel());

            staffLine.get(j).add(checkBox.get(j));
            staffLine.get(j).add(staff.get(j));
            staffPanel.add(staffLine.get(j));
        }

        for (int j=0; j<nchordVoices; j++) {
            checkBox.add(new JCheckBox("Chords " + (j+1), true));
            staffLine.add(new JPanel());

            staffLine.get(nvoices+j).add(checkBox.get(nvoices+j));
            staffLine.get(nvoices+j).add(chordStaff.get(j));
            staffPanel.add(staffLine.get(nvoices+j));
        }
    }

    // resize Staff panel and Staffs when parameters change
    private void sizeStaffPanels() {
        staffPanel.removeAll();
        staffPanel.setPreferredSize(
                new Dimension((16*beatsPer*measures)+160,
                    (nvoices+nchordVoices)*160));
        for (int i=0; i < nvoices; i++)
            staff.get(i).setPreferredSize(
                    new Dimension(60+16*beatsPer*measures,145));
        for (int i=0; i < nchordVoices; i++)
            chordStaff.get(i).setPreferredSize(
                    new Dimension(60+16*beatsPer*measures,145));

        scrollPane.setViewportView(staffPanel);
    }

    // save piece as MMM file
    private void saveMMM(String filename) {
        try (FileWriter fw = new FileWriter(filename + ".MMM")) {
            fw.write("beatsPer: " + beatsPer + "\n");
            fw.write("measures: " + measures + "\n");
            fw.write("tempo: " + tempo + "\n");
            fw.write("voices: " + nvoices + "\n");
            fw.write("chordVoices: " + nchordVoices + "\n");
            fw.write("chords: " + chords.size() + "\n");
            for (int j = 0; j < chords.size(); j++) {
                int[] notes = chords.get(j).getNotes();
                fw.write(chords.get(j).getName() + " "
                        + chords.get(j).getSymbol());
                for (int i = 0; i < notes.length; i++)
                    fw.write(" " + notes[i]);
                fw.write("\n");
            }
            fw.write("END HEADER\n");
            for (int j = 0; j < nvoices; j++) {
                for (int i = 0; i < beatsPer*measures; i++) 
                    fw.write(staff.get(j).getNoteAndType(i) + " ");
                fw.write("\n");
            }
            for (int j = 0; j < nchordVoices; j++) {
                for (int i = 0; i < beatsPer*measures; i++) 
                    fw.write(chordStaff.get(j).getNoteAndTypeAndQuality(i) + " ");
                fw.write("\n");
            }
        } catch (IOException e) {
            System.out.println("Error writing to file " + filename + ".MMM");
        }
    }

    // begin composing a new piece
    private void newPiece() {
        // read in parameters:
        JTextField bpField = new JTextField("4",2);
        JTextField measField = new JTextField("8",2);
        JTextField tempoField = new JTextField("100",3);
        JPanel initPanel = new JPanel(new GridLayout(4,1));
        JPanel line1 = new JPanel();
        JPanel line2 = new JPanel();
        JPanel line3 = new JPanel();
        JPanel line4 = new JPanel();
        line2.add(new JLabel("Beats per measure:",JLabel.RIGHT));
        line2.add(bpField);
        line3.add(new JLabel("             Measures:",JLabel.RIGHT));
        line3.add(measField);
        line4.add(new JLabel("          Tempo (bpm):",JLabel.RIGHT));
        line4.add(tempoField);
        initPanel.add(line1);
        initPanel.add(line2);
        initPanel.add(line3);
        initPanel.add(line4);
        JOptionPane.showConfirmDialog(null, initPanel, 
                "Please please:", JOptionPane.OK_CANCEL_OPTION);
        beatsPer = Integer.parseInt(bpField.getText());
        measures = Integer.parseInt(measField.getText());
        tempo    = Integer.parseInt(tempoField.getText());

        playStart = 1;
        playEnd = measures;

        nvoices = 1;
        nchordVoices = 0;

        voice.add(new Piece(tempo));
        staff.add(new Staff());
        staff.get(0).setParams(beatsPer,measures,voice.get(0));

        // define the three built-in chords:
        chords.add(new Chord("MAJOR","M",new int[]{0,4,7}));
        chords.add(new Chord("MINOR","m",new int[]{0,3,7}));
        chords.add(new Chord("DOMSEVENTH","7",new int[]{0,4,7,10}));

        initSwingLists();
    }

    // choose a MMM file
    private String chooseFile() throws IOException {
        JFileChooser chooser = new JFileChooser(
                new File(System.getProperty("user.dir")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Major's Midi Maker files", "MMM", "mmm");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(jfrm);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to open this file: " +
                    chooser.getSelectedFile().getName());
            return chooser.getSelectedFile().getName();
        } else {
            return "rathernot";
        }
    }

    // set tempo
    private void setTempo() {
        JTextField tempoField = new JTextField("" + tempo);
        JPanel line1 = new JPanel();
        line1.add(new JLabel("Enter new tempo: "));
        line1.add(tempoField);

        JOptionPane.showConfirmDialog(null, line1, 
                "Please please:", JOptionPane.OK_CANCEL_OPTION);
        tempo = Integer.parseInt(tempoField.getText());
    }

    // remove a Voice of a ChordVoice
    private int removeVoice(String type, int length) {
        Integer[] choices = new Integer[length];
        for (int i=0; i<length; i++)
            choices[i] = new Integer(i+1);

        JComboBox<Integer> cb = new JComboBox<Integer>(choices);
        cb.setSelectedItem(choices[choices.length-1]);
        JPanel line1 = new JPanel();
        line1.add(new JLabel("Remove " + type + " number:"));
        line1.add(cb);

        int result = JOptionPane.showConfirmDialog(null, line1, 
                "Please please:", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
            return (int)(cb.getSelectedItem())-1;
        else
            return -1;
    }

    // set play interval
    private void setInterval() {
        JTextField startField = new JTextField("" + playStart,3);
        JTextField endField = new JTextField("" + playEnd,3);
        JPanel intervalPanel = new JPanel(new GridLayout(2,1));
        JPanel line1 = new JPanel();
        JPanel line2 = new JPanel();
        line1.add(new JLabel("From measure: ",JLabel.RIGHT));
        line1.add(startField);
        line2.add(new JLabel("   To measure: ",JLabel.RIGHT));
        line2.add(endField);
        intervalPanel.add(line1);
        intervalPanel.add(line2);
        JOptionPane.showConfirmDialog(null, intervalPanel, 
                "Please please:", JOptionPane.OK_CANCEL_OPTION);
        playStart = Integer.parseInt(startField.getText());
        playEnd = Integer.parseInt(endField.getText());
    }

    // load MMM file (call readMMM and then set up panels accordingly)
    private int loadMMM() throws IOException {

        String filename = chooseFile();

        if (!(filename.equals("rathernot"))) {

            clearLists();

            readHeader(filename);

            int[] notesAndTypes = new int[2*nvoices*measures*beatsPer];
            int[] chordsAndTypes = new int[2*nchordVoices*measures*beatsPer];
            int[] chordQualities = new int[nchordVoices*measures*beatsPer];

            readMMM(filename, notesAndTypes, chordsAndTypes, chordQualities);

            for (int j = 0; j < nvoices; j++) {
                voice.add(new Piece(tempo));
                staff.add(new Staff());
                staff.get(j).setParams(beatsPer,measures,voice.get(j));
                for (int i = 0; i < measures*beatsPer; i++) 
                    staff.get(j).setNoteAndType(i,notesAndTypes[2*j*measures*beatsPer+2*i],
                            notesAndTypes[2*j*measures*beatsPer+2*i+1]);
            }
            for (int j = 0; j < nchordVoices; j++) {
                chordVoice.add(new Prog(tempo));
                chordStaff.add(new ChordStaff(chords));
                chordStaff.get(j).setParams(beatsPer,measures,chordVoice.get(j));
                for (int i = 0; i < measures*beatsPer; i++) 
                    chordStaff.get(j).setChordAndTypeAndQuality(i,
                            chordsAndTypes[2*j*measures*beatsPer+2*i],
                            chordsAndTypes[2*j*measures*beatsPer+2*i+1],
                            chordQualities[2*j*measures*beatsPer+i]);
            }

            playStart = 1;
            playEnd = measures;

            initSwingLists();

            return 0;
        } else {
            return -1;
        }
    }

    private void loadMMM(String fn) throws IOException {
        // strip extension from filename and add MMM
        String filename = fn.split("\\.")[0];

        readHeader(filename + ".MMM");

        int[] notesAndTypes = new int[2*nvoices*measures*beatsPer];
        int[] chordsAndTypes = new int[2*nchordVoices*measures*beatsPer];
        int[] chordQualities = new int[nchordVoices*measures*beatsPer];

        readMMM(filename + ".MMM", notesAndTypes, chordsAndTypes, chordQualities);

        for (int j = 0; j < nvoices; j++) {
            voice.add(new Piece(tempo));
            staff.add(new Staff());
            staff.get(j).setParams(beatsPer,measures,voice.get(j));
            for (int i = 0; i < measures*beatsPer; i++) 
                staff.get(j).setNoteAndType(i,notesAndTypes[2*j*measures*beatsPer+2*i],
                        notesAndTypes[2*j*measures*beatsPer+2*i+1]);
        }
        for (int j = 0; j < nchordVoices; j++) {
            chordVoice.add(new Prog(tempo));
            chordStaff.add(new ChordStaff(chords));
            chordStaff.get(j).setParams(beatsPer,measures,chordVoice.get(j));
            for (int i = 0; i < measures*beatsPer; i++) 
                chordStaff.get(j).setChordAndTypeAndQuality(i,
                        chordsAndTypes[2*j*measures*beatsPer+2*i],
                        chordsAndTypes[2*j*measures*beatsPer+2*i+1],
                        chordQualities[2*j*measures*beatsPer+i]);
        }

        playStart = 1;
        playEnd = measures;

        initSwingLists();
    }

    private void readHeader(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String[] words = br.readLine().split(" ");
            beatsPer = Integer.parseInt(words[1]);

            words = br.readLine().split(" ");
            measures = Integer.parseInt(words[1]);

            words = br.readLine().split(" ");
            tempo = Integer.parseInt(words[1]);

            words = br.readLine().split(" ");
            nvoices = Integer.parseInt(words[1]);

            words = br.readLine().split(" ");
            nchordVoices = Integer.parseInt(words[1]);

            words = br.readLine().split(" ");
            int nchords = Integer.parseInt(words[1]);

            // read chord patterns
            for (int i = 0; i < nchords; i++) {
                words = br.readLine().split(" ");
                String name = words[0];
                String symbol = words[1];
                int[] notes = new int[words.length-2];
                for (int j = 0; j < words.length-2; j++)
                    notes[j] = Integer.parseInt(words[2+j]);
                chords.add(new Chord(name, symbol, notes));
                /* System.out.println("chord " + i
                   + "\nname " + name
                   + "\nsymbol " + symbol
                   + "\nnotes " + Arrays.toString(notes)
                   + "\n"); */
            }

            System.out.println(br.readLine());

        } catch (IOException e) {
            System.out.println("Error reading header of file " + filename + ".MMM");
            System.exit(0);
        }
    }

    // read selected MMM file
    private void readMMM(String filename, int[] notesAndTypes, 
            int[] chordsAndTypes, int[] chordQualities) 
        throws IOException {
        try (BufferedReader br = new BufferedReader(
                    new FileReader(filename))) {

            // skip through header
            do {
            } while (!(br.readLine().equals("END HEADER")));

            // read content
            for (int j = 0; j < nvoices; j++) {
                String[] words = br.readLine().split(" ");
                for (int i = 0; i < 2*beatsPer*measures; i++)
                    notesAndTypes[2*j*beatsPer*measures+i] 
                        = Integer.parseInt(words[i]);
            }
            for (int j = 0; j < nchordVoices; j++) {
                String[] words = br.readLine().split(" ");
                for (int i = 0; i < beatsPer*measures; i++) {
                    chordsAndTypes[2*j*beatsPer*measures+2*i] 
                        = Integer.parseInt(words[3*i]);
                    chordsAndTypes[2*j*beatsPer*measures+2*i+1] 
                        = Integer.parseInt(words[3*i+1]);
                    chordQualities[j*beatsPer*measures+i] 
                        = Integer.parseInt(words[3*i+2]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file " + filename + ".MMM");
        }
    }

    // listener for buttons and menus
    public void actionPerformed(ActionEvent ae) {
        // find out who detected the event
        String comStr = ae.getActionCommand();

        // Buttons!
        if (comStr.equals("Tempo")) {
            setTempo();
            for (int i = 0; i<nvoices; i++)
                voice.get(i).setTempo(tempo);
            for (int i = 0; i<nchordVoices; i++)
                chordVoice.get(i).setTempo(tempo);
        } else if (comStr.equals("Voice+")) {
            nvoices++;
            voice.add(new Piece(tempo));
            staff.add(new Staff());
            staff.get(nvoices-1).setParams(
                    beatsPer,measures,voice.get(nvoices-1));
            initSwingLists();
            jfrm.repaint();
        } else if (comStr.equals("Voice-")) {
            if (nvoices > 0) {
                int sel = removeVoice("voice", nvoices);
                if (sel >= 0) {
                    voice.remove(sel);
                    staff.remove(sel);
                    nvoices--;
                    initSwingLists();
                    jfrm.repaint();
                }
            }
        } else if (comStr.equals("Chords+")) {
            nchordVoices++;
            chordVoice.add(new Prog(tempo));
            chordStaff.add(new ChordStaff(chords));
            chordStaff.get(nchordVoices-1).setParams(
                    beatsPer,measures,chordVoice.get(nchordVoices-1));
            initSwingLists();
            jfrm.repaint();
        } else if (comStr.equals("Chords-")) {
            if (nchordVoices > 0) {
                int sel = removeVoice("chord voice", nchordVoices);
                if (sel >= 0) {
                    chordVoice.remove(sel);
                    chordStaff.remove(sel);
                    nchordVoices--;
                    initSwingLists();
                    jfrm.repaint();
                }   
            }
        } else if (comStr.equals("Play") || comStr.equals("Play interval")) {

            if (comStr.equals("Play")) {
                playStart = 1; 
                playEnd = measures;
            } else {
                setInterval();
            }

            allTogether.empty();
            for (int j=0; j<nvoices; j++) {
                if (checkBox.get(j).isSelected()) {
                    staff.get(j).build((playStart-1)*beatsPer,playEnd*beatsPer);
                    allTogether.add(new Part(voice.get(j).getPhrase(),
                                "Part " + j, 
                                JMC.PIANO));
                    //System.out.println("Voice " + j + " is a go!");
                }
            }
            for (int j=0; j<nchordVoices; j++) {
                if (checkBox.get(nvoices+j).isSelected()) {
                    chordStaff.get(j).build((playStart-1)*beatsPer,playEnd*beatsPer);
                    for (int i=0; i<4; i++)
                        allTogether.add(new Part(chordVoice.get(j).getPhrase(i),
                                    "Part " + j + " voice " + i, 
                                    JMC.PIANO));
                    //System.out.println("Chords " + j + " are a go!");
                }
            }
            // spawn a thread to play in the background!
            PlayScore playIt = new PlayScore("playMidi", allTogether);
            Thread playThrd = new Thread(playIt);
            playThrd.start();
            // View.show(allTogether);

        } else if (comStr.equals("New")) {
            clearLists();
            newPiece();
            jfrm.revalidate();
            jfrm.repaint();

        } else if (comStr.equals("Open")) {
            try {
                loadMMM();
            } catch (IOException exc) {
                System.err.println("IO error: " + exc);
                System.exit(0);
            }
            jfrm.repaint();

        } else if (comStr.equals("Save")) {
            String filename = JOptionPane.showInputDialog(
                    "Please enter a filename");
            for (int j=0; j<nvoices; j++) {
                staff.get(j).build(0, beatsPer*measures);
                allTogether.add(new Part(voice.get(j).getPhrase(),
                            "Part " + j, 
                            JMC.PIANO));
            }
            for (int j=0; j<nchordVoices; j++) { 
                chordStaff.get(j).build(0, beatsPer*measures);
                for (int i=0; i<4; i++)
                    allTogether.add(new Part(chordVoice.get(j).getPhrase(i),
                                "Part " + j + " voice " + i, 
                                JMC.PIANO));
            }
            // save to midi file
            Write.midi(allTogether, filename + ".mid");
            // save to proprietary ascii format
            saveMMM(filename);

        } else if (comStr.equals("Exit")) {
            System.exit(0);
        } else if (comStr.equals("Instructions")) {
            JDialog helpDialog = new JDialog(jfrm,"How to use MMM");

            JLabel helpText = new JLabel("<html><p align=left>"
                    + "To add a natural note to a voice,<br>"
                    + "left-click on the musical staff.<br><br>"
                    + "To add a sharp note, hold down the CTRL key<br>"
                    + "and left-click.<br><br>"
                    + "To add a flat note, hold down the SHIFT key<br>"
                    + "and left-click.<br><br>"
                    + "To insert a rest, right-click on the staff.<br><br>"
                    + "To add a chord to a chord progression,<br>"
                    + "enter the tonic note as above.<br><br>"
                    + "Subsequent clicks at the same position<br>"
                    + "cycle through the available chord qualities:<br>"
                    + "    (M)ajor, (m)inor, dominant (7)th,<br>"
                    + "or user-defined chords.<br><br>"
                    );

            helpDialog.setLayout(new BorderLayout());

            helpDialog.add(helpText);
            helpDialog.setLocationRelativeTo(jfrm);
            helpDialog.getRootPane().setBorder(
                    BorderFactory.createEmptyBorder(30,30,30,30));
            helpDialog.pack();
            helpDialog.setVisible(true);

        } else if (comStr.equals("About")) {

            JDialog helpDialog = new JDialog(jfrm,"About MMM");

            JLabel helpText = new JLabel("<html><p align=center>"
                    + "The Major's Midi Maker<br><br><br>"
                    + "Copyright (c) 2015<br>"
                    + "by Mark D. Fruman<br>"
                    );

            helpDialog.setLayout(new BorderLayout());

            helpDialog.add(helpText);
            helpDialog.setLocationRelativeTo(jfrm);
            helpDialog.getRootPane().setBorder(
                    BorderFactory.createEmptyBorder(30,30,30,30));
            helpDialog.pack();
            helpDialog.setVisible(true);

        } else if (comStr.equals("WWW")) {
            // to do: openFromWeb();
            jfrm.repaint();
        }
        // Menus!
    }

    // constructor (and body of program)
    MidiPlayer(String[] args) {

        jfrm = new JFrame("MidiPlayer");

        mainPanel = new JPanel();

        mainPanel.setBackground(Color.PINK);

        staffLine = new ArrayList<JPanel>(0);
        checkBox = new ArrayList<JCheckBox>(0);

        chords = new ArrayList<Chord>(0);

        voice = new ArrayList<Piece>(0);
        chordVoice = new ArrayList<Prog>(0);
        staff = new ArrayList<Staff>(0);
        chordStaff = new ArrayList<ChordStaff>(0);

        // panel to hold musical staffs
        staffPanel = new JPanel();
        // sideways scrolling JScrollPane for longer pieces
        scrollPane = new JScrollPane(staffPanel);

        // if command line argument try to open file
        if (args.length > 0) {
            try {
                loadMMM(args[0]);
            } catch (IOException exc) {
                System.out.println("File not found or not valid MMM file.");
                System.exit(0);
            }
        } else {
            // On launch create dialog to choose new file or load from disk
            Object[] options = {"New piece", "Load file"};
            int newOrOld = JOptionPane.showOptionDialog(null, "How to begin?", "Welcome!",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (newOrOld == 0) {
                newPiece();
            } else {
                try {
                    int openfile = loadMMM();
                    if (openfile != 0)
                        newPiece();
                } catch (IOException exc) {
                    System.err.println("IO error: " + exc);
                    System.exit(0);
                }
            }
        }

        JLabel titleLabel = new JLabel("<html><p align=left>The Major's<br>Midi Maker");
        titleLabel.setFont(new Font("SansSerif",Font.BOLD,24));
        titleLabel.setForeground(Color.BLUE);
        titleLabel.setVerticalAlignment(SwingConstants.TOP);

        // Buttons and Listeners
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.PAGE_AXIS));

        JButton voiceAddButton = new JButton("Voice+");
        voiceAddButton.addActionListener(this);

        JButton voiceRemButton = new JButton("Voice-");
        voiceRemButton.addActionListener(this);

        JButton chordVoiceAddButton = new JButton("Chords+");
        chordVoiceAddButton.addActionListener(this);

        JButton chordVoiceRemButton = new JButton("Chords-");
        chordVoiceRemButton.addActionListener(this);

        JButton playButton = new JButton("Play from top");
        playButton.setActionCommand("Play");
        playButton.addActionListener(this);

        JButton playIntervalButton = new JButton("Play interval");
        playIntervalButton.setActionCommand("Play interval");
        playIntervalButton.addActionListener(this);
        playIntervalButton.setPreferredSize(playButton.getPreferredSize());

        // assemble Swing components
        scrollPane.setPreferredSize(new Dimension(700, 510));

        buttonPanel.setPreferredSize(new Dimension(170, 510));

        buttonPanel.setBackground(mainPanel.getBackground());

        buttonPanel.add(titleLabel);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,40)));

        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playIntervalButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(playButton);
        buttonPanel.add(playIntervalButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,20)));

        voiceAddButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        voiceRemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(voiceAddButton);
        buttonPanel.add(voiceRemButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,10)));

        chordVoiceAddButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chordVoiceRemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(chordVoiceAddButton);
        buttonPanel.add(chordVoiceRemButton);

        // add an empty border to buttonPanel and scrollPane (spacing)
        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(10,10,0,30));
        scrollPane.setBorder(
                BorderFactory.createEmptyBorder(5,5,5,5));

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(buttonPanel,BorderLayout.WEST);
        mainPanel.add(scrollPane,BorderLayout.EAST);

        // Menus!
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu playMenu = new JMenu("Play");
        JMenu composeMenu = new JMenu("Compose");
        JMenu helpMenu = new JMenu("Help");

        // File menu
        fileMenu.setMnemonic('F');
        JMenuItem fileNewItem = new JMenuItem("New");
        fileNewItem.addActionListener(this);
        JMenuItem fileOpenItem = new JMenuItem("Open ...");
        fileOpenItem.setActionCommand("Open");
        fileOpenItem.addActionListener(this);
        JMenuItem fileSaveItem = new JMenuItem("Save as ...");
        fileSaveItem.setActionCommand("Save");
        fileSaveItem.addActionListener(this);
        JMenuItem fileWebOpenItem = new JMenuItem("Open from Web ...");
        fileWebOpenItem.setActionCommand("WWW");
        fileWebOpenItem.addActionListener(this);
        JMenuItem fileExitItem = new JMenuItem("Exit");
        fileExitItem.addActionListener(this);
        fileMenu.add(fileNewItem);
        fileMenu.add(fileOpenItem);
        fileMenu.add(fileSaveItem);
        fileMenu.addSeparator();
        fileMenu.add(fileWebOpenItem);
        fileMenu.addSeparator();
        fileMenu.add(fileExitItem);

        // Play menu
        playMenu.setMnemonic('P');
        JMenuItem playFromTopItem = new JMenuItem("Play from the top");
        playFromTopItem.setActionCommand("Play");
        playFromTopItem.addActionListener(this);
        JMenuItem playIntervalItem = new JMenuItem("Play interval ...");
        playIntervalItem.setActionCommand("Play interval");
        playIntervalItem.addActionListener(this);
        playMenu.add(playFromTopItem);
        playMenu.add(playIntervalItem);

        // Compose menu
        composeMenu.setMnemonic('C');
        JMenuItem setTempoItem = new JMenuItem("Change tempo");
        setTempoItem.setActionCommand("Tempo");
        setTempoItem.addActionListener(this);
        JMenuItem addVoiceItem = new JMenuItem("Add voice");
        addVoiceItem.setActionCommand("Voice+");
        addVoiceItem.addActionListener(this);
        JMenuItem removeVoiceItem = new JMenuItem("Remove voice ...");
        removeVoiceItem.setActionCommand("Voice-");
        removeVoiceItem.addActionListener(this);
        JMenuItem addChordItem = new JMenuItem("Add chord line");
        addChordItem.setActionCommand("Chords+");
        addChordItem.addActionListener(this);
        JMenuItem removeChordItem = new JMenuItem("Remove chord line ...");
        removeChordItem.setActionCommand("Chords-");
        removeChordItem.addActionListener(this);
        JMenuItem editChordsItem = new JMenuItem("Edit chords ...");
        editChordsItem.setActionCommand("Edit chords");
        editChordsItem.addActionListener(this);
        composeMenu.add(setTempoItem);
        composeMenu.addSeparator();
        composeMenu.add(addVoiceItem);
        composeMenu.add(removeVoiceItem);
        composeMenu.addSeparator();
        composeMenu.add(addChordItem);
        composeMenu.add(removeChordItem);
        composeMenu.addSeparator();
        composeMenu.add(editChordsItem);

        // Help menu
        helpMenu.setMnemonic('H');
        JMenuItem helpInstItem = new JMenuItem("Instructions");
        helpInstItem.addActionListener(this);
        JMenuItem helpAboutItem = new JMenuItem("About MMM");
        helpAboutItem.setActionCommand("About");
        helpAboutItem.addActionListener(this);
        helpMenu.add(helpInstItem);
        helpMenu.addSeparator();
        helpMenu.add(helpAboutItem);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(playMenu);
        menuBar.add(composeMenu);
        menuBar.add(helpMenu);

        // Make jfrm use FlowLayout (default anyway)
        jfrm.setLayout(new FlowLayout());
        jfrm.getContentPane().setBackground(Color.PINK);

        // arrange main frame
        jfrm.setSize(900, 590);
        jfrm.setResizable(false);
        jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // add menubar and main panel to the frame
        jfrm.setJMenuBar(menuBar);
        jfrm.getContentPane().add(mainPanel);

        // make it visible!
        jfrm.setVisible(true);
    }

    public static void main(String[] args) {
        final String[] commandLineArgs = args;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MidiPlayer(commandLineArgs);
            }
        });
    }
}
