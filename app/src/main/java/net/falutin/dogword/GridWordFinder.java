package net.falutin.dogword;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sokolov on 3/1/2015.
 */
public class GridWordFinder {

    private final LetterTree tree;
    private final int minLength;

    GridWordFinder (LetterTree tree) {
        this (tree, 3);
    }

    GridWordFinder (LetterTree tree, int minLength) {
        this.tree = tree;
        this.minLength = minLength;
    }

    public Set<String> findWords (Char2d grid) {
        HashSet<String> words = new HashSet<>();
        int size = grid.width() * grid.height();
        StringBuilder chars = new StringBuilder(size);
        assert (size < 32);  // we're using a bitmask in an integer to keep track of visited cells
        for (int row = 0; row < grid.height(); row++) {
            for (int col = 0; col < grid.width(); col++) {
                findWords(words, grid, row, col, 0, chars);
            }
        }
        return words;
    }

    public void findWords(Set<String> words, Char2d grid, int row, int col, int visited, StringBuilder letters) {
        int pos = (1 << (row * grid.width() + col));
        if ((visited & pos) != 0) {
            return;
        }
        visited |= pos;
        char c = grid.get(row, col);
        letters.append(Character.toLowerCase(c));
        int wordFound = tree.lookup(letters);
        if (letters.length() >= minLength && (wordFound & LetterTree.IS_WORD) != 0) {
            words.add(letters.toString());
        }
        if (wordFound > 1) {
            findNearbyWords(words, grid, row, col, visited, letters);
        }
        letters.setLength(letters.length()-1);
    }

    private void findNearbyWords(Set<String> words, Char2d grid, int row, int col, int visited, StringBuilder letters) {
        for (int dr = -1; dr <= 1; dr++) {
            int rr = row + dr;
            if (rr < 0 || rr >= grid.width()) {
                continue;
            }
            for (int dc = -1; dc <= 1; dc++) {
                int cc = col + dc;
                if (cc < 0 || cc >= grid.height()) {
                    continue;
                }
                findWords(words, grid, rr, cc, visited, letters);
            }
        }
    }

}
