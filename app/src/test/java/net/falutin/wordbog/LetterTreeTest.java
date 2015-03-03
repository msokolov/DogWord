package net.falutin.wordbog;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class LetterTreeTest {

    @Test
    public void testLookup () throws IOException {
        DynamicLetterTrie trie = new DynamicLetterTrie();
        trie.add(new StringReader("park\nparking\nparty"));
        LetterTree tree = LetterTree.build(trie);
        assertEquals(2, tree.lookup("par"));
        assertEquals(3, tree.lookup("park"));
        assertEquals(1, tree.lookup("party"));
        assertEquals(0, tree.lookup("partying"));
        assertEquals(0, tree.lookup("xxx"));
        assertEquals(0, tree.lookup("x"));
    }

    public static String getWordFilePath() {
        final String pwd = System.getProperty("user.dir");
        return pwd + "/src/test/resources/WORDS500";
    }

    public static LetterTree readLetterTree () throws IOException {
        // FIXME - have to use abs paths here because android studio doesn't handle test resources
        // properly (see https://code.google.com/p/android/issues/detail?id=136013)
        InputStream in = new FileInputStream(new File(getWordFilePath()));
        assertNotNull(in);
        InputStreamReader reader = new InputStreamReader(in);
        DynamicLetterTrie dlt = new DynamicLetterTrie();
        dlt.add(reader);
        reader.close();
        return LetterTree.build(dlt);
    }

    @Test
    public void testReadWrite () throws IOException {
        LetterTree tree = readLetterTree();
        assertEquals(3, tree.lookup("encyclical"));
        assertEquals(2, tree.lookup("encycli"));
        assertEquals(0, tree.lookup("encyclion"));
        // WORDS file is 1.67 MB; we build a tree with 389308 ints which is 1.49 MB
        // WORD gzips to 445K.  We should be able to get much better compression using
        // an finite-state automaton that collapses suffixes as well, and squeezes out
        // some extra fat from the micro-data representation
        final String binPath = getWordFilePath() + ".bin";
        DataOutputStream out = new DataOutputStream(new FileOutputStream(binPath));
        tree.write(out);
        out.close();
        DataInputStream bin = new DataInputStream(new FileInputStream(binPath));
        LetterTree tree2 = LetterTree.read(bin);
        bin.close();
        assertEquals(3, tree2.lookup("encyclical"));
        assertEquals(2, tree2.lookup("encycli"));
        assertEquals(0, tree2.lookup("encyclion"));
    }

}
