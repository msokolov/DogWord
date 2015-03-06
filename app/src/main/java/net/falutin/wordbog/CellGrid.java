package net.falutin.wordbog;

/**
 * Holds the grid letters
 */
public class CellGrid implements Char2d {

    private final int width, height;
    private final char cells[];
    private final char letterBag[];

    public CellGrid (int width, int height) {
        this.width = width;
        this.height = height;
        cells = new char[width * height];
        letterBag = new char[] {'A','A','A','A','A','A','A','A','A','B','B','C','C','D','D','D','D',
                'E','E','E','E','E','E','E','E','E','E','E','E','F','F','G','G','G','H','H',
                'I','I','I','I','I','I','I','I','I','J','K','L','L','L','L','M','M',
                'N','N','N','N','N','N','O','O','O','O','O','O','O','O','P','P','Q',
                'R','R','R','R','R','R','S','S','S','S','T','T','T','T','T','T','U','U','U','U',
                'V','V','W','W','X','Y','Y','Z'};
    }

    public void randomize() {
        for (int i = cells.length - 1; i >= 0; i--) {
            cells[i] = getRandomChar();
        }
    }

    private char getRandomChar() {
        return letterBag[(int) (Math.random()*letterBag.length)];
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
