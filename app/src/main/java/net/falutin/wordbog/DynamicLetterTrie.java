package net.falutin.wordbog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by sokolov on 3/1/2015.
 *
 * A letter trie that supports incremental updating; used as a temporary structure while
 * creating a LetterTree.
 */
public class DynamicLetterTrie {

    Node root = new Node();

    /**
     * Reads a list of words, expecting one word per line, adding all the words to the tree.
     * @param in the word list, as a Reader
     */
    public void add(Reader in) throws IOException {
        BufferedReader inb = new BufferedReader(in);
        String line;
        while ((line = inb.readLine()) != null) {
            Node.add(line, root);
        }
    }

    public Node getRoot () {
        return root;
    }

    public static class Node {
        TreeMap<Character, Node> children;
        boolean isTerminal;

        Node() {
            children = new TreeMap<>();
            isTerminal = false;
        }

        static void add(String s, Node node) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                Node child = node.children.get(c);
                if (child == null) {
                    child = new Node();
                    node.children.put(c, child);
                }
                node = child;
            }
            node.isTerminal = true;
        }
    }
}
