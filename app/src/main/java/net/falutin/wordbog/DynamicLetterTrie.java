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
            root.add(line);
        }
    }

    public Node getRoot () {
        return root;
    }

    /** Converts the tree into a DAG by merging identical suffixes */
    public void collapseSuffixes () {
        root.collapse (new StringBuilder(), new HashMap<CharSequence,Node>());
    }

    public static class Node {
        TreeMap<Character, Node> children;
        boolean isTerminal;

        Node() {
            children = new TreeMap<>();
            isTerminal = false;
        }

        void add(String s) {
            Node node = this;
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

        /**
         * @param suffixMap map of suffix to Node encoding the suffix, updated by this method
         * and used to update the tree by collapsing common suffixes.
         * @param suff partial suffix built up while descending unique branches of the tree
         * @return the number of terminal nodes (ie words) on this branch
         */
        int collapse (StringBuilder suff, Map<CharSequence,Node> suffixMap) {
            int terminalCount = isTerminal ? 1 : 0;
            if (children.size() == 1) {
                Map.Entry<Character,Node> child = children.firstEntry();
                suff.append (child.getKey());
                terminalCount += child.getValue().collapse (suff, suffixMap);
            } else if (children.size() > 1) {
                terminalCount += collapseChildren(suff, suffixMap);
            }
            return terminalCount;
        }

        private int collapseChildren(StringBuilder suff, Map<CharSequence, Node> suffixMap) {
            int terminalCount = 0;
            TreeMap<Character, Node> collapsed = null;
            for (Map.Entry<Character,Node> child : children.entrySet()) {
                char c = child.getKey();
                suff.append(c);
                int childTerminalCount = child.getValue().collapse (suff, suffixMap);
                if (childTerminalCount == 1) {
                    if (suffixMap.containsKey(suff)) {
                        if (collapsed == null) {
                            collapsed = new TreeMap<>(children);
                        }
                        collapsed.put(c, suffixMap.get(suff));
                    } else {
                        suffixMap.put (suff, child.getValue());
                    }
                }
                terminalCount += childTerminalCount;
                suff.setLength(0);
            }
            if (collapsed != null) {
                children = collapsed;
            }
            return terminalCount;
        }

    }

}
