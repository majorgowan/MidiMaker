package midiplayer;

// Class for chord objects
class Chord {

    private String name;
    private String symbol;
    private int[] notes = new int[6];

    Chord() {
        name = "New chord";
        symbol = "Ch";
        notes[0] = 0;
        for (int i = 1; i<notes.length; i++)
            notes[i] = 100;
    }

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
