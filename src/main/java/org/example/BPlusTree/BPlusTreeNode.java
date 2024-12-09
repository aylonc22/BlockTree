package org.example.BPlusTree;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNode {
    public boolean isLeaf;
    public List<Integer> keys;
    public List<String> values; // Only for leaf nodes
    public List<Integer> childrenOffsets; // Only for internal nodes
    public int offset;
    public final int order;
    public BPlusTreeNode nextLeaf; // Points to the next leaf node (only used in leaf nodes)

    public BPlusTreeNode(boolean isLeaf, int offset, int order) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.childrenOffsets = new ArrayList<>();
        this.offset = offset;
        this.order = order;
        this.nextLeaf = null; // Default to null
    }

    @Override
    public String toString() {
        return "BPlusTreeNode{" +
                "isLeaf=" + isLeaf +
                ", keys=" + keys +
                ", values=" + values +
                ", childrenOffsets=" + childrenOffsets +
                ", offset=" + offset +
                '}';
    }

    /**
     * Calculate the end offset of the node in the ByteBuffer.
     *
     * @return The end offset of the node in the ByteBuffer.
     */
    public int getEndOffset() {
        return offset + getSize();
    }

    /**
     * Calculate the size of the node based on its type and content.
     *
     * @return The size of the node in bytes.
     */
    public int getSize() {
        int keySize = 4; // Assuming integer keys
        int offsetSize = 4; // Assuming integer offsets
        int valueSize = 15; // Adjust based on max value length

        int maxKeys = order - 1; // Maximum number of keys
        int nodeSize;

        if (isLeaf) {
            // Size calculation for leaf nodes
            nodeSize = (maxKeys * keySize) + (maxKeys * valueSize) + 4; // Adding space for nextLeaf pointer (4 bytes)
        } else {
            // Size calculation for internal nodes
            nodeSize = (maxKeys * keySize) + (order * offsetSize) + (offsetSize * (maxKeys + 1));
        }

        return nodeSize;
    }

    public static BPlusTreeNode deserialize(ByteBuffer buffer, int offset, int order) {
        // System.out.println("deserializing node at offset " + offset);
        buffer.position(offset);
        boolean isLeaf = buffer.get() == 1;
        BPlusTreeNode node = new BPlusTreeNode(isLeaf, offset, order);

        int keyCount = buffer.getInt();
        for (int i = 0; i < keyCount; i++) {
            node.keys.add(buffer.getInt());
        }

        if (isLeaf) {
            for (int i = 0; i < keyCount; i++) {
                int valueLength = buffer.getInt();
                byte[] valueBytes = new byte[valueLength];
                buffer.get(valueBytes);
                node.values.add(new String(valueBytes));
            }

            int nextLeafOffset = buffer.getInt();  // Offset of the next leaf node
            // For leaf nodes, deserialize the nextLeaf pointer (if it exists)
            if (nextLeafOffset != -1) {
                node.nextLeaf = deserialize(buffer, nextLeafOffset, order);
            }

        } else {
            for (int i = 0; i <= keyCount; i++) {
                node.childrenOffsets.add(buffer.getInt());
            }
        }

        // System.out.println(node);
        return node;
    }

    public void serialize(ByteBuffer buffer) {
        // System.out.print("Serializing node with " + keys.size() + " keys at offset " + buffer.position());
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putInt(keys.size());
        for (int key : keys) {
            buffer.putInt(key);
        }

        if (isLeaf) {
            for (String value : values) {
                byte[] valueBytes = value.getBytes();
                buffer.putInt(valueBytes.length);
                buffer.put(valueBytes);
            }

            // Serialize the nextLeaf pointer (if exists)
            if (nextLeaf != null) {
                buffer.putInt(nextLeaf.offset);  // Assuming you store the next leaf's offset
            } else {
                buffer.putInt(-1);  // Indicating no next leaf
            }

        } else {
            for (int offset : childrenOffsets) {
                buffer.putInt(offset);
            }
        }
    }
}
