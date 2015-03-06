package midiplayer;

// Swing packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

// For resizable arrays
import java.util.ArrayList;

// For reading and writing files
import java.io.*;
import javax.imageio.*;
import java.net.URL;

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
            ClassLoader cl = this.getClass().getClassLoader();
            URL trebleImage = cl.getResource("Images/treble.png");

            tc_img = ImageIO.read(trebleImage);
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

    public void resetChords(ArrayList<Chord> ch) {
        chords = ch;
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

