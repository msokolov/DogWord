package net.falutin.wordbog;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

/**
 * Holds the grid letters
 */
public class CellGrid implements Char2d {

    private final int width, height;
    private final char cells[];
    private final char permutedLetters[];
    private final Random random;
    private static final Character[] LETTERS = new Character[] {
            'A','A','A','A','A','A','A','A','A','B','B','C','C','D','D','D','D',
            'E','E','E','E','E','E','E','E','E','E','E','F','F','G','G','G','H','H',
            'I','I','I','I','I','I','I','I','J','K','L','L','L','L','M','M',
            'N','N','N','N','N','N','O','O','O','O','O','O','O','O','P','P','Q',
            'R','R','R','R','R','R','S','S','S','S','T','T','T','T','T','T','U','U','U','U',
            'V','V','W','W','X','Y','Y','Z'};

    public CellGrid (int width, int height) {
        this.width = width;
        this.height = height;
        cells = new char[width * height];
        random = new Random();
        permutedLetters = permuteLetters();
    }

    private char[] permuteLetters() {
        assert(LETTERS.length == 6 * 16);
        char[] pLetters = new char[LETTERS.length];
        LinkedList<Character> letterBag = new LinkedList<>();
        letterBag.addAll(Arrays.asList(LETTERS));
        // permute the letters
        for (int i = 0; i < LETTERS.length; i++) {
            pLetters[i] = letterBag.remove(random.nextInt(letterBag.size()));
        }
        return pLetters;
    }

    public void randomize() {
        for (int i = cells.length - 1; i >= 0; i--) {
            cells[i] = getRandomChar(i);
        }
    }

    private char getRandomChar(int cellIndex) {
        // return permutedLetters[random.nextInt(permutedLetters.length)];
        // choose from a different bag for each cell
        int ncells = 16;
        return permutedLetters[ncells * random.nextInt(LETTERS.length / ncells) + cellIndex];
    }

    public char get(int idx) {
        return cells[idx];
    }

    @Override
    public char get(int row, int col) {
        return cells[width * row + col];
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    public String toString () {
        return new String(cells);
    }

    public void setCells (String cellString) {
        System.arraycopy(cellString.toCharArray(), 0, cells, 0, width * height);
    }

}
