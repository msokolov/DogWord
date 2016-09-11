package net.falutin.dogword;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GridWordsTest {

    private static final String[] WORDS = new String[] { "a", "b", "c", "dd", "e", "f", "g", "h", "i" };
    private GridWords gridWords;

    @Before
    public void setUp() {
        gridWords = new GridWords(WORDS);
    }

    @Test
    public void testRevealWord() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g"});
        assertEquals("a, b, c, [dd], e, f, g, [h], [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testRevealLen() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g"});
        assertEquals("a, b, c, (2), e, f, g, (1), (1)",
                gridWords.formatWordList(HintStyle.Length, 3));
    }

    @Test
    public void testRunOf4() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g", "h"});
        assertEquals("a, b, c, [dd], e, f, g, h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testRunOf6() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "dd", "e", "f"});
        assertEquals("a, b, c, dd, e, f, [g], [h], [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testRunOf7() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "dd", "e", "f", "g"});
        assertEquals("a, b, c · e, f, g, [h], [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testRunOf9() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "dd", "e", "f", "g", "h", "i"});
        assertEquals("a, b, c · g, h, i",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testInitialGap() {
        gridWords.addFoundWords(new String[] { "b", "c", "dd", "e", "f", "g", "h"});
        assertEquals("[a], b, c, dd · f, g, h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 3));
    }

    @Test
    public void testRevealWordL1() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g"});
        assertEquals("a · c, [dd], e · g, [h], [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }


    @Test
    public void testRunOf4L1() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g", "h"});
        assertEquals("a · c, [dd], e · h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }

    @Test
    public void testRunOf6L1() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "e", "f", "g", "h"});
        assertEquals("a · c, [dd], e · h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }

    @Test
    public void testRunOf7L1() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "dd", "e", "f", "g"});
        assertEquals("a · g, [h], [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }

    @Test
    public void testRunOf9L1() {
        gridWords.addFoundWords(new String[] { "a", "b", "c", "dd", "e", "f", "g", "h", "i"});
        assertEquals("a · i",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }

    @Test
    public void testRunOf2L1() {
        gridWords.addFoundWords(new String[] { "a", "b", "dd", "e", "g", "h"});
        assertEquals("a, b, [c], dd, e, [f], g, h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }

    @Test
    public void testInitialGapL1() {
        gridWords.addFoundWords(new String[] { "b", "c", "dd", "e", "f", "g", "h"});
        assertEquals("[a], b · h, [i]",
                gridWords.formatWordList(HintStyle.RevealWord, 1));
    }
}
