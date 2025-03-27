package it.cnr.isti.labsedc.concern;

public class WordOccurrence {
    private String word;
    private int count;

    public WordOccurrence(String word, int count) {
        this.word = word;
        this.count = count;
    }

    public String getWord() {
        return word;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "(" + word + "," + count + ")";
    }
}
