package midiplayer;

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
