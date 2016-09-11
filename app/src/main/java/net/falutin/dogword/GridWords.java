package net.falutin.dogword;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GridWords {

    private static final String COMMA = ", ";
    //private static final String ELLIPSIS = ", …,";
    private static final String ELLIPSIS = " · ";

    private final HashSet<String> wordsFound;
    private String[] words;

    public GridWords(GridWordFinder finder, CellGrid grid) {
        this(findAllWords(finder, grid));
    }

    GridWords(String[] words) {
        this.words = words;
        wordsFound = new HashSet<>();
    }

    private static String[] findAllWords (GridWordFinder finder, CellGrid grid) {
        Set<String> wordSet = finder.findWords(grid);
        String[] words = wordSet.toArray(new String[wordSet.size()]);
        Arrays.sort(words);
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toUpperCase();
        }
        return words;
    }

    public void addFoundWords(String[] wf) {
        wordsFound.addAll(Arrays.asList(wf));
    }

    public String[] getWordsFound() {
        return wordsFound.toArray(new String[wordsFound.size()]);
    }

    public int getNumFound() {
        return wordsFound.size();
    }

    public boolean isFound(String word) {
        return wordsFound.contains(word);
    }

    public void addFound(String word) {
        wordsFound.add(word);
    }

    public int getSize() {
        return words.length;
    }

    // return a string listing all the words in alphabetical order, with unfound words represented
    // by a string of underscores.
    public String formatWordList(HintStyle hintStyle, int len) {
        StringBuilder buf = new StringBuilder();
        // runStart is the index of the first discovered word after the last hidden word
        int i = 0, runStart = 0;
        for (String word : words) {
            boolean collapsing = (i - runStart) >= len;
            if (buf.length() != 0 && !collapsing) {
                buf.append(COMMA);
            }
            if (!wordsFound.contains(word)) {
                appendCollapsed(buf, i, runStart, len);
                switch (hintStyle) {
                    case RevealWord:
                        buf.append('[').append(word).append(']');
                        break;
                    case Length:
                        buf.append("(").append(word.length()).append(")");
                        break;
                    case None:
                }
                runStart = i + 1;
            } else if (!collapsing) {
                buf.append(word);
            }
            ++i;
        }
        appendCollapsed(buf, i, runStart, len);
        return buf.toString();
    }

    private void appendCollapsed(StringBuilder buf, int i, int runStart, int len) {
        boolean collapsing = (i - runStart) >= len;
        if (!collapsing) {
            return;
        }
        // show the words just before this, if we were collapsing
        // show up to 3, but don't overlap with words already shown
        // and include an ellipsis if any words were collapsed
        int runLength = i - runStart;
        int gapEndIndex;
        if (runLength > 2 * len) {
            buf.append(ELLIPSIS);
            gapEndIndex = i - len;
        } else {
            gapEndIndex = i - runLength + len;
            buf.append(COMMA);
        }
        for (int j = gapEndIndex; j < i; j++) {
            buf.append(words[j]);
            if (j < words.length - 1) {
                buf.append(COMMA);
            }
        }
    }
}