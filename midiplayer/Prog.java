package midiplayer;

// JMusic packages
import jm.JMC;
import jm.music.data.*;
import jm.util.*;

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
