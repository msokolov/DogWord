package net.falutin.wordbog;

/**
 * Holds the grid letters
 */
public class CellGrid implements Char2d {

    private final int width, height;
    private final char cells[];

    public CellGrid (int width, int height) {
        this.width = width;
        this.height = height;
        cells = new char[width * height];
    }

    public void randomize() {
        for (int i = cells.length - 1; i >= 0; i--) {
            cells[i] = getRandomChar();
        }
    }

    private char getRandomChar() {
        // TODO: weight distribution by english letter frequencies
        return (char) ('A' + (int)(Math.random() * 26));
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

}
