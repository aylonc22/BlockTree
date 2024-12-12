package org.example.BPlusTree;

import com.sun.jdi.InvalidTypeException;
import org.example.Config.Config;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNode<T extends Comparable<T>> {
    public boolean isLeaf;
    public List<T> keys;
    public List<String> values; // Only for leaf nodes
    public List<Integer> childrenOffsets; // Only for internal nodes
    public int offset;
    public final int order;
    public BPlusTreeNode<T> nextLeaf; // Points to the next leaf node (only used in leaf nodes)

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
        int keySize = Config.keySize; // Size of each key
        int offsetSize = Config.offsetSize; // Size of each offset
        int valueSize = Config.valueSize; // Maximum size of each value (adjust as needed)
        int nextLeaf = Config.nextLeaf;
        int maxKeys = order - 1; // Maximum number of keys
        int nodeSize;

        if (isLeaf) {
            // Size calculation for leaf nodes
            nodeSize = (maxKeys * keySize) + (maxKeys * valueSize) + nextLeaf; // Adding space for nextLeaf pointer (4 bytes)
        } else {
            // Size calculation for internal nodes
            nodeSize = (maxKeys * keySize) + (order * offsetSize) + (offsetSize * (maxKeys + 1));
        }

        return nodeSize;
    }

    public static <T extends Comparable<T>> BPlusTreeNode<T> deserialize(ByteBuffer buffer, int offset, int order,Class<T> type){
        // System.out.println("deserializing node at offset " + offset);
        buffer.position(offset);
        boolean isLeaf = buffer.get() == 1;

        BPlusTreeNode<T> node = new BPlusTreeNode<>(isLeaf, offset, order);

        int keyCount = buffer.getInt();
        for (int i = 0; i < keyCount; i++) {
           if(type == Integer.class)
                node.keys.add(type.cast(buffer.getInt()));
           else{
               int keyLength = buffer.getInt();
               byte[] keyBytes = new byte[keyLength];
               buffer.get(keyBytes);
               node.keys.add(type.cast(new String(keyBytes)));
           }
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
                node.nextLeaf = deserialize(buffer, nextLeafOffset, order,type);
            }

        } else {
            for (int i = 0; i <= keyCount; i++) {
                node.childrenOffsets.add(buffer.getInt());
            }
        }

        // System.out.println(node);
        return node;
    }

    public void serialize(ByteBuffer buffer) throws InvalidTypeException {
        // System.out.print("Serializing node with " + keys.size() + " keys at offset " + buffer.position());
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putInt(keys.size());
        for (T key : keys) {
           if(key instanceof Integer) {
               buffer.putInt((Integer) key);
           }
           else if(key instanceof String){
               byte[] keyBytes = ((String) key).getBytes();
               buffer.putInt(keyBytes.length);
               buffer.put(keyBytes);
           }
           else {
               throw new InvalidTypeException("Unsupported key type");
           }
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
