
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
