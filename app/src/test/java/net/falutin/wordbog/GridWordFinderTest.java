package net.falutin.wordbog;

import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Created by sokolov on 3/1/2015.
 */
public class GridWordFinderTest {

    @Test
    public void testFindWords () throws IOException {
        LetterTree tree = LetterTreeTest.readLetterTree(false);
        TestGrid grid = new TestGrid("ABCDEFGHIJKLMNOP");
        GridWordFinder finder = new GridWordFinder(tree);
        Set<String> words = finder.findWords(grid);
        assertEquals (16, words.size());
        for (String word : new String[] { "fie", "fin", "fink", "fino",
                "glop", "ink", "jin", "jink", "knife", "knop", "kop", "lop", "mink", "nim",
                "plonk", "pol"}) {
            assertTrue (word + " not found", words.contains(word));
        }
        finder = new GridWordFinder(tree, 4);
        words = finder.findWords(grid);
        assertEquals (8, words.size());
        for (String word : new String[] { "fink", "fino", "glop", "jink", "knife", "knop", "mink", "plonk"}) {
            assertTrue (words.contains(word));
        }
    }

    class TestGrid implements Char2d {
        char[] grid = new char[16];

        public TestGrid(String s) {
            grid = s.substring(0, 16).toCharArray();
        }

        @Override
        public int width() {
            return 4;
        }

        @Override
        public int height() {
            return 4;
        }

        @Override
        public char get(int row, int col) {
            return grid[row * 4 + col];
        }
    }
}
