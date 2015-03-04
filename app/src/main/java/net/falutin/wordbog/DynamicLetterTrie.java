package net.falutin.wordbog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.HashMap;
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

        void add(String s, Node node) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                Node child = children.get(c);
                if (child == null) {
                    child = new Node();
                    children.put(c, child);
                }
                node = child;
            }
            isTerminal = true;
        }

        /**
         * @param suffixMap map of suffix to Node encoding the suffix, updated by this method
         * and used to update the tree by collapsing common suffixes.
         * @param suff partial suffix built up while descending unique branches of the tree
         * @return the number of terminal nodes (ie words) on this branch
         */
        int collapse (StringBuilder suff, Map<CharSequence, Node> suffixMap) {
            // TODO -- figure out how to accumulate suffixes efficiently
            // perhaps passing in a StringBuilder
            int terminalCount = isTerminal ? 1 : 0;
            if (children.size() == 1) {
                Map.Entry<Character,Node> child = children.firstEntry();
                suff.append (child.getKey());
                terminalCount += child.getValue().collapse (suff, suffixMap);
            } else if (children.size() > 1) {
                for (Map.Entry<Character,Node> child = children.entrySet()) {
                    terminalCount += child.getValue().collapse (child.getKey(), suff);
                    suff.setLength(0);
                }
            }
            if (terminalCount == 1) {
                addOrCollapse (suff.toString(), suffixMap);
            }
            return terminalCount;
        }

    }

    /** Converts the tree into a DAG by merging identical suffixes */
    public void collapseSuffixes () {
        HashMap<CharSequence,Node> suffixMap = new HashMap<>();
        root.collapse (suffixMap);
    }

}
