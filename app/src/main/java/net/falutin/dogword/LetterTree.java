package net.falutin.dogword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * LetterTree provides fast lookup of word prefixes in a compact data structure. It is essentially
 * a prefix tree (trie) or DAG, optimized for storing a tree of 1-byte letters in small space with fast
 * lookup.
 * Created by sokolov on 3/1/2015.
 */
public class LetterTree {

    public final static byte IS_WORD = 1;
    private final static int EOL = 0x80000000; // end of list
    private final static int EOW = 0x40000000; // end of word
    private final static int NODE_MASK = 0x3ffffff0;
    private final static int NODE_SHIFT = 8;

    /**
     * The dictionary is encoded as an array of ints, each of which represents an edge in a tree
     * of nodes. Logically, each node contains the following information:
     * <ul>
     *   <li>A boolean indicating whether the node is terminal, marking the end of a word.</li>
     *   <li>A map of children: letter-> node</li>
     * </ul>
     * The tree is encoded using a list of single 32-bit integers as follows:
     * <ul>
     *     <li>One byte stores a character</li>
     *     <li>One bit marks end-of-word</li>
     *     <li>One bit marks end-of-child-list</li>
     *     <li>The remaining bits encode an offset to a child node</li>
     * </ul>
     */
    private int[] nodes;
    private int count;

    public LetterTree () {
        this (8192);
    }

    public LetterTree (int capacity) {
        count = 0;
        nodes = new int[Math.max(capacity, 256)];
    }

    public int getNodeCount() {
        return count;
    }

    public boolean contains (CharSequence letters) {
        return (lookup(letters) & IS_WORD) == 1;
    }

    /**
     * @param letters a string of letters to look up
     * @return a bitmask that is non-zero if the string occurs in the tree, and
     * has the 1-bit set if if the string is a terminal node (ie a word).
     */
    public int lookup(CharSequence letters) {
        int offset = 0;
        int node = 0;
        for (int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);
            assert (c <= 0xff);
            byte b = (byte) c;
            for (;;) {
                node = nodes[offset];
                byte letter = getNodeLetter(node);
                if (letter < b) {
                    if (isLastChild(node)) {
                        return 0;
                    }
                    offset++;
                } else if (letter > b) {
                    return 0;
                } else {
                    offset = getFirstChildIndex(node);
                    break;
                }
            }
            if (offset == 0 && i < letters.length() - 1) {
                // last matching node  has no children, and there are still letters to match
                return 0;
            }
        }
        return (isWord(node) ? 1 : 0) | (offset > 0 ? 2 : 0);
    }
    private void addNodeStorage (int moreNodes) {
        if (count + moreNodes > nodes.length) {
            expand();
        }
        count += moreNodes;
    }

    private void expand() {
        int [] newNodes = new int[nodes.length * 2];
        System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
        nodes = newNodes;
    }

    private void shrink() {
        int [] newNodes = new int[count];
        System.arraycopy(nodes, 0, newNodes, 0, count);
        nodes = newNodes;
    }

    public static LetterTree build (DynamicLetterTrie trie) {
        // recursively build the tree by adding all the nodes and setting references while
        // unwinding the recursion
        LetterTree letterTree = new LetterTree();
        letterTree.build(trie.getRoot(), 0);
        letterTree.shrink();
        return letterTree;
    }

    public static LetterTree buildDAG (DynamicLetterTrie trie) {
        // recursively build the tree by adding all the nodes and setting references while
        // unwinding the recursion
        int nodeCount = trie.collapseSuffixes();
        LetterTree letterTree = new LetterTree();
        int idMap[] = new int[nodeCount];
        letterTree.buildDAG(trie.getRoot(), 0, idMap);
        letterTree.shrink();
        return letterTree;
    }

    private int buildDAG (DynamicLetterTrie.Node node, int offset, int[] idMap) {
        int nChildren = node.children.size();
        // allocate space for the children of this node
        addNodeStorage(nChildren);
        // and set the node pointer in the following empty space
        int nextOffset = offset + nChildren;
        int childIndex = 0;
        for (Character c : node.children.keySet()) {
            DynamicLetterTrie.Node child = node.children.get(c);
            // store zero as the firstChildOffset if this child has no children
            int nextChildOffset = idMap[child.id];
            //System.out.println(String.format ("%c %d %d %d=>%d", c, offset + childIndex, nextOffset, child.id, nextChildOffset));
            if (nextChildOffset == 0 && !child.children.isEmpty()) {
                // store the child, as represented by its grandchildren
                nextChildOffset = idMap[child.id] = nextOffset;
                nextOffset = buildDAG(child, nextOffset, idMap);
            }
            nodes[offset + childIndex] = encodeNode(c, nextChildOffset, childIndex == nChildren-1, child.isTerminal);
            ++childIndex;
        }
        return nextOffset;
    }

    private int build (DynamicLetterTrie.Node node, int offset) {
        int nChildren = node.children.size();
        // allocate space for the children of this node
        addNodeStorage(nChildren);
        // and set the node pointer in the following empty space
        int nextOffset = offset + nChildren;
        int childIndex = 0;
        for (Character c : node.children.keySet()) {
            DynamicLetterTrie.Node child = node.children.get(c);
            // store zero as the firstChildOffset if this child has no children
            int nextChildOffset = child.children.isEmpty() ? 0 : nextOffset;
            nodes[offset + childIndex] = encodeNode(c, nextChildOffset, childIndex == nChildren-1, child.isTerminal);
            ++childIndex;
            nextOffset = build(child, nextOffset);
        }
        return nextOffset;
    }

    private int encodeNode (char c, int nodeIndex, boolean isLastChild, boolean isTerminal) {
        assert(c <= 0xff);
        return c | (isLastChild ? EOL : 0) | (isTerminal ? EOW : 0) | (nodeIndex << NODE_SHIFT);
    }

    private byte getNodeLetter (int node) {
        return (byte) (node & 0xff);
    }

    private int getFirstChildIndex (int node) {
        return (node & NODE_MASK) >>> NODE_SHIFT;
    }

    private boolean isLastChild (int node) {
        return (node & EOL) != 0;
    }

    private boolean isWord (int node) {
        return (node & EOW) != 0;
    }

    @Override
    public String toString () {
        return String.format("LetterTree<%d>", count);
    }

    /*
     * read from a binary file in trie format
     */
    public static LetterTree read(DataInputStream in) throws IOException {
        int count = in.readInt();
        LetterTree tree = new LetterTree(count);
        tree.count = count;
        for (int i = 0; i < count; i++) {
            // see comment in write for performance note
            tree.nodes[i] = in.readInt();
        }
        return tree;
    }

    public void write (DataOutputStream out) throws IOException {
        out.writeInt(count);
        for (int i = 0; i < count; i++) {
            // there is no bulk write for int[]?  Apparently you can do better with nio.FileChannel
            out.writeInt(nodes[i]);
        }
    }

}
