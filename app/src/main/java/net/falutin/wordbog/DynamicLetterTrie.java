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

    /** Converts the tree into a DAG by merging identical suffixes
     * @return the number of nodes in the resulting DAG */
    public int collapseSuffixes () {
        root.collapse (new ReverseStringBuilder(32), new HashMap<String,Node>());
        return root.identify(0);
    }

    public static class Node {
        int id;
        TreeMap<Character, Node> children;
        boolean isTerminal;

        Node() {
            id = -1;
            children = new TreeMap<>();
            isTerminal = false;
        }

        public void add(String s) {
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
         * Walks the tree of nodes, assigning contiguous id numbers to them
         * @param nextId the next id to assign before the function call
         * @return the next id to assign after the function call
         */
        public int identify(int nextId) {
            id = nextId++;
            for (Node child : children.values()) {
                nextId = child.identify(nextId);
            }
            return nextId;
        }

        /**
         * @param suffixMap map of suffix to Node encoding the suffix, updated by this method
         * and used to update the tree by collapsing common suffixes.
         * @param suff partial suffix built up while descending unique branches of the tree
         * @return the number of terminal nodes (ie words) on this branch
         */
        private int collapse(ReverseStringBuilder suff, Map<String, Node> suffixMap) {
            int terminalCount = isTerminal ? 1 : 0;
            TreeMap<Character, Node> collapsed = null;
            for (Map.Entry<Character,Node> child : children.entrySet()) {
                char c = child.getKey();
                // we pre-pend chars to the suffix as we return from the recursion. We clear
                // before recursing down so that subsequent children start with a clean slate
                suff.clear();
                int childTerminalCount = child.getValue().collapse (suff, suffixMap);
                if (childTerminalCount == 1) {
                    suff.insert(c);
                    String suffix = suff.toString();
                    if (suffixMap.containsKey(suffix)) {
                        if (collapsed == null) {
                            collapsed = new TreeMap<>(children);
                        }
                        collapsed.put(c, suffixMap.get(suffix));
                    } else {
                        suffixMap.put (suffix.toString(), child.getValue());
                    }
                }
                terminalCount += childTerminalCount;
            }
            if (collapsed != null) {
                children = collapsed;
            }
            return terminalCount;
        }
/*
        public String toString() {
            StringBuilder b = new StringBuilder();
            toString (b, 0);
            return b.toString();
        }

        private void toString(StringBuilder b, int indent) {
            for (Map.Entry<Character,Node> entry : children.entrySet()) {
                for (int i = 0; i < indent; i++) {
                    b.append(' ');
                }
                b.append(entry.getKey()).append('\n');
                entry.getValue().toString(b, indent+1);
            }
        }
*/
    }

    /**
     * Fixed-capacity CharSequence that accepts characters at the beginning.
     */
    public static class ReverseStringBuilder implements CharSequence {

        private char [] buf;
        private int start;

        ReverseStringBuilder(int size) {
            buf = new char[size];
            start = size;
        }

        public void insert (char c) {
            buf[--start] = c;
        }

        public void deleteChar () {
            if (start >= buf.length) {
                throw new IllegalStateException ("No chars to delete");
            }
            ++ start;
        }

        public void clear() {
            start = buf.length;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new String(buf, this.start + start, this.start + end);
        }

        @Override
        public int length() {
            return buf.length - start;
        }

        @Override
        public char charAt(int index) {
            return buf[start + index];
        }

        @Override
        public String toString() {
            return new String(buf, start, buf.length-start);
        }
    }

}
