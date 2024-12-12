package org.example.BPlusTree;

import com.sun.jdi.InvalidTypeException;
import org.example.Config.Config;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * A B+ Tree implementation with an arena allocator for efficient memory management.
 * The B+ Tree supports insertion, deletion, and search operations.
 */
public class BPlusTree<T extends Comparable<T>> implements Iterable<Map.Entry<T, String>> {
    private static final int DEFAULT_ORDER = 3; // Default order (maximum number of children per node)
    private static final int DEFAULT_MB = 1; // Default memory size (in megabytes) for the tree
    private ByteBuffer buffer; // Byte buffer to store the serialized nodes
    private BPlusTreeNode<T> root; // Root node of the B+ Tree
    private int order; // Order of the B+ Tree
    public int lastAllocatedEndOffset = -1;
    private Set<Integer> printedOffsets = new HashSet<>();
    private Class<T> type;
    /**
     * Default constructor initializing the B+ Tree with default memory size and order.
     */
    public BPlusTree(Class<T> type) throws InvalidTypeException {
        this(DEFAULT_MB, DEFAULT_ORDER,type);
    }

    /**
     * Constructor to initialize the B+ Tree with specified memory size and order.
     *
     * @param MB The memory size in megabytes.
     * @param order The order of the B+ Tree.
     */
    public BPlusTree(int MB, int order,Class<T> type) throws InvalidTypeException {
        if (MB < 1) {
            throw new IllegalArgumentException("Memory must be 1 MB or more");
        }
        if (order < 3) {
            throw new IllegalArgumentException("Order must be 3 or more");
        }
        if(!type.equals(Integer.class) && !type.equals(String.class))
        {
            throw new InvalidTypeException("Unsupported key Type");
        }
        // Allocate buffer with the given memory size and set byte order
        this.buffer = ByteBuffer.allocate((1024 * 1024) * MB);
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        this.order = order;
        this.type = type;
        // Initialize the root as a leaf node and serialize it
        this.root = new BPlusTreeNode<>(true, allocateNode(true),order);
        serializeNode(root);
    }
    @Override
    public Iterator<Map.Entry<T, String>> iterator(){
        return new Iterator<Map.Entry<T, String>>() {
            private BPlusTreeNode<T> currentLeaf;  // Start at the leftmost leaf

            {
                try {
                    currentLeaf = findLeftmostLeaf(root);
                } catch (InvalidTypeException e) {
                    throw new RuntimeException(e);
                }
            }

            private int currentIndex = 0;  // Current index in the leaf node

            @Override
            public boolean hasNext() {
                // Check if there are more elements to iterate over.
                return currentLeaf != null && (currentIndex < currentLeaf.keys.size() || currentLeaf.nextLeaf != null);
            }

            @Override
            public Map.Entry<T, String> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in the tree.");
                }

                // Get the current key-value pair
                T key =  currentLeaf.keys.get(currentIndex);
                String value =  currentLeaf.values.get(currentIndex);
                Map.Entry<T, String> entry = new AbstractMap.SimpleEntry<>(key, value);

                // Move to the next index, or to the next leaf if necessary
                currentIndex++;
                if (currentIndex >= currentLeaf.keys.size()) {
                    currentLeaf = currentLeaf.nextLeaf;  // Move to the next leaf
                    currentIndex = 0;
                }

                return entry;
            }

            // Find the leftmost leaf node starting from the root
            private BPlusTreeNode<T> findLeftmostLeaf(BPlusTreeNode<T> node) throws InvalidTypeException {
                while (!node.isLeaf) {
                    // Traverse down to the leftmost child
                    node = BPlusTreeNode.deserialize(buffer,  node.childrenOffsets.get(0),order,type);
                }
                return node;
            }
        };
    }

    // Generate the hashcode based on the tree’s contents
    @Override
    public int hashCode() {
        int result = 1;
        for (Map.Entry<T, String> entry : this) {
            // Hashing the key
            result = 31 * result + (entry.getKey() == null ? 0 : entry.getKey().hashCode());

            // Hashing the value (String)
            result = 31 * result + (entry.getValue() == null ? 0 : entry.getValue().hashCode());
        }
        return result;
    }

    /**
     * Allocate space for a new node in the buffer.
     *
     * @return The position where the node is allocated.
     */
    private int allocateNode(boolean isLeaf) throws InvalidTypeException {
        // Calculate the size of a node based on the order
        int maxKeys = order - 1; // Maximum number of keys in the node
        int keySize = Config.keySize; // Size of each key (assuming integer keys)
        int offsetSize = Config.offsetSize; // Size of each offset (assuming integer offsets)
        int valueSize = Config.valueSize; // Maximum size of each value (adjust as needed)
        int nextLeaf = Config.nextLeaf;

        int nodeSize;
        if (isLeaf) {
            // Size calculation for leaf nodes
            nodeSize = (maxKeys * keySize) + (maxKeys * valueSize) + nextLeaf;
        } else {
            // Size calculation for internal nodes
            nodeSize = (maxKeys * keySize) + ((order * offsetSize) + (offsetSize * (maxKeys + 1)));
        }

        // Allocate space for the node
        BPlusTreeNode<T> node = BPlusTreeNode.deserialize(buffer,root!=null?lastAllocatedEndOffset:0,order,type);
        int position = node.getEndOffset();
        if (position + nodeSize > buffer.capacity()) {
            throw new RuntimeException("Buffer capacity exceeded during node allocation");
        }
        buffer.position(position + nodeSize); // Allocate space for the node
        lastAllocatedEndOffset = buffer.position();
        return node.offset;
    }

    /**
     * Insert multiple key-value pairs into the B+ Tree.
     *
     * @param items A map containing key-value pairs to be inserted.
     */
    public void insertMany(HashMap<T, String> items) throws InvalidTypeException {
        for (var item : items.entrySet()) {
            insert(item.getKey(), item.getValue());
        }
    }

    /**
     * Insert a key-value pair into the B+ Tree.
     *
     * @param key The key to insert.
     * @param value The value associated with the key.
     */
    public void insert(T key, String value) throws InvalidTypeException {
        BPlusTreeNode<T> leaf = findLeaf(root, key,type);
        int index = leaf.keys.indexOf(key);
        if (index != -1) {
            // Update the value if key already exists
            leaf.values.set(index, value);
            serializeNode(leaf);
        } else if (leaf.keys.size() < order - 1) {
            // Insert the key-value pair into the leaf node
            insertInLeaf(leaf, key, value,type);
        } else {
            // Split the leaf node if it is full
            splitLeaf(leaf, key, value,type);
        }
    }

    /**
     * Find the leaf node where a key should be located.
     *
     * @param node The current node.
     * @param key The key to find.
     * @return The leaf node containing the key.
     */
    private <K extends Comparable<K>> BPlusTreeNode<K> findLeaf(BPlusTreeNode<K> node, K key, Class<K> type) throws InvalidTypeException {
        while (!node.isLeaf) {
            int i = 0;
            // Compare based on the type of T
            while (i < node.keys.size()) {
                if (type == Integer.class) {
                    // Perform Integer comparison if T is Integer
                    Integer currentKey = (Integer) node.keys.get(i);
                    if ((Integer) key>= currentKey) {
                        break;
                    }
                } else if (type == String.class) {
                    // Perform String comparison if T is String
                    String currentKey = (String) node.keys.get(i);
                    if (key.compareTo(type.cast(currentKey)) < 0) {
                        break;
                    }
                }
                i++;
            }

            // Get the child node's offset
            int childOffset = node.childrenOffsets.get(i);
            // Deserialize the child node
            node = BPlusTreeNode.deserialize(buffer, childOffset, order,type);
        }
        return node;
    }

    /**
     * Insert a key-value pair into a leaf node.
     *
     * @param leaf The leaf node.
     * @param key The key to insert.
     * @param value The value associated with the key.
     */
    private void insertInLeaf(BPlusTreeNode<T> leaf, T key, String value, Class<T> type) throws InvalidTypeException {
        int index = 0;

        // Check the type of T
        if (type == Integer.class) {
            // Handle the case where the key is an Integer
            Integer keyAsInt = (Integer) key;
            while (index < leaf.keys.size() && keyAsInt > (Integer) leaf.keys.get(index)) {
                index++;
            }
        } else if (type == String.class) {
            // Handle the case where the key is a String
            String keyAsString = (String) key;
            while (index < leaf.keys.size() && keyAsString.compareTo((String) leaf.keys.get(index)) > 0) {
                index++;
            }
        } else {
            // You can throw an exception or handle other types
            throw new InvalidTypeException("Unsupported key type");
        }

        // Insert the key-value pair at the correct index
        leaf.keys.add(index, key);
        leaf.values.add(index, value);

        // Serialize the node after insertion
        serializeNode(leaf);
    }


    /**
     * Split a leaf node and distribute its keys and values between the original and new leaf nodes.
     *
     * @param leaf The leaf node to split.
     * @param key The key to insert into the leaf.
     * @param value The value associated with the key.
     */
    private void splitLeaf(BPlusTreeNode<T> leaf, T key, String value, Class<T> type) throws InvalidTypeException {
        int t = (order - 1) / 2; // Number of keys in each split node
        BPlusTreeNode<T> newLeaf = new BPlusTreeNode<>(true, allocateNode(true), order);

        // Prepare lists to redistribute keys and values
        List<T> allKeys = new ArrayList<>(leaf.keys);
        List<String> allValues = new ArrayList<>(leaf.values);

        int insertIndex = getInsertIndex(key, type, allKeys);

        // Insert the key-value pair at the correct index
        allKeys.add(insertIndex, key);
        allValues.add(insertIndex, value);

        // Split the keys and values between the old and new leaf nodes
        newLeaf.keys.addAll(allKeys.subList(t + 1, allKeys.size()));
        newLeaf.values.addAll(allValues.subList(t + 1, allValues.size()));
        leaf.keys = new ArrayList<>(allKeys.subList(0, t + 1));
        leaf.values = new ArrayList<>(allValues.subList(0, t + 1));

        // Update the nextLeaf pointers after splitting
        newLeaf.nextLeaf = leaf.nextLeaf;  // The new leaf points to the next leaf node (if any)
        leaf.nextLeaf = newLeaf;  // The old leaf points to the new leaf node

        // Update the root if necessary
        if (leaf == root) {
            BPlusTreeNode<T> newRoot = new BPlusTreeNode<>(false, allocateNode(false), order);
            newRoot.keys.add(newLeaf.keys.get(0));
            newRoot.childrenOffsets.add(leaf.offset);
            newRoot.childrenOffsets.add(newLeaf.offset);
            root = newRoot;
            serializeNode(newRoot);
        } else {
            BPlusTreeNode<T> parent = findParent(root, leaf);
            int index = parent.childrenOffsets.indexOf(leaf.offset);
            parent.keys.add(index, newLeaf.keys.get(0));
            parent.childrenOffsets.add(index + 1, newLeaf.offset);

            // Split the internal node if needed
            if (parent.keys.size() > order - 1) {
                splitInternalNode(parent);
            } else {
                serializeNode(parent);
            }
        }

        // Serialize the leaf and new leaf nodes after splitting
        serializeNode(leaf);
        serializeNode(newLeaf);
    }

    private static <T extends Comparable<T>> int getInsertIndex(T key, Class<T> type, List<T> allKeys) throws InvalidTypeException {
        int insertIndex = 0;

        // Handle key comparison based on the type of key (Integer or String)
        if (type == Integer.class) {
            // Integer comparison
            Integer keyAsInt = (Integer) key;
            while (insertIndex < allKeys.size() && keyAsInt > (Integer) allKeys.get(insertIndex)) {
                insertIndex++;
            }
        } else if (type == String.class) {
            // String comparison
            String keyAsString = (String) key;
            while (insertIndex < allKeys.size() && keyAsString.compareTo((String) allKeys.get(insertIndex)) > 0) {
                insertIndex++;
            }
        } else {
            // Handle unsupported types
            throw new InvalidTypeException("Unsupported key type");
        }
        return insertIndex;
    }


    /**
     * Split an internal node and distribute its keys and children between the original and new internal nodes.
     *
     * @param node The internal node to split.
     */
    private void splitInternalNode(BPlusTreeNode<T> node) throws InvalidTypeException {
        int t = (order - 1) / 2; // Number of keys in each split node
        BPlusTreeNode<T> newInternal = new BPlusTreeNode<>(false, allocateNode(false),order);

        // Calculate the middle index
        int mid = t;

        // Move the keys and children to the new node
        newInternal.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newInternal.childrenOffsets.addAll(node.childrenOffsets.subList(mid + 1, node.childrenOffsets.size()));

        // Adjust the current node
        node.keys = new ArrayList<>(node.keys.subList(0, mid + 1));
        node.childrenOffsets = new ArrayList<>(node.childrenOffsets.subList(0, mid + 1));

        if (node == root) {
            // Create a new root
            BPlusTreeNode<T> newRoot = new BPlusTreeNode<>(false, allocateNode(false),order);
            newRoot.keys.add(node.keys.get(mid));
            newRoot.childrenOffsets.add(node.offset);
            newRoot.childrenOffsets.add(newInternal.offset);
            root = newRoot;
            serializeNode(newRoot);
        } else {
            BPlusTreeNode<T> parent = findParent(root, node);
            int index = parent.childrenOffsets.indexOf(node.offset);
            parent.keys.add(index, node.keys.get(mid));
            parent.childrenOffsets.add(index + 1, newInternal.offset);

            if (parent.keys.size() > order - 1) {
                splitInternalNode(parent);
            } else {
                serializeNode(parent);
            }
        }
        serializeNode(node);
        serializeNode(newInternal);
    }

    /**
     * Find the parent of a given child node.
     *
     * @param node The current node.
     * @param child The child node.
     * @return The parent node, or null if not found.
     */
    private BPlusTreeNode<T> findParent(BPlusTreeNode<T> node, BPlusTreeNode<T> child) throws InvalidTypeException {
        if (!node.isLeaf && node.childrenOffsets.contains(child.offset)) {
            return node;
        }
        for (Integer offset : node.childrenOffsets) {
            BPlusTreeNode<T> n = BPlusTreeNode.deserialize(buffer, offset,order,type);
            BPlusTreeNode<T> foundNode = findParent(n, child);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }

    /**
     * Search for a key in the B+ Tree and return its associated value.
     *
     * @param key The key to search for.
     * @return The value associated with the key, or null if the key is not found.
     */
    public String search(T key) throws InvalidTypeException {
        BPlusTreeNode<T> leaf = findLeaf(root, key,type);
        int index = leaf.keys.indexOf(key);
        return index != -1 ? leaf.values.get(index) : null;
    }

    /**
     * Delete a key from the B+ Tree.
     *
     * @param key The key to delete.
     */
    public void delete(T key) throws InvalidTypeException {
        BPlusTreeNode<T> leaf = findLeaf(root, key,type);
        int index = leaf.keys.indexOf(key);

        if (index != -1) {
            leaf.keys.remove(index);
            leaf.values.remove(index);
            // Handle underflow if necessary
            if (leaf.keys.size() < (order - 1) / 2 && leaf != root) {
                handleUnderflow(leaf);
            }
        }
        serializeNode(leaf);
    }

    /**
     * Handle the underflow situation in a node by either borrowing from or merging with siblings.
     *
     * @param node The node with underflow.
     */
    private void handleUnderflow(BPlusTreeNode<T> node) throws InvalidTypeException {
        BPlusTreeNode<T> parent = findParent(root, node);
        int index = parent.childrenOffsets.indexOf(node.offset);

        if (index > 0) {
            BPlusTreeNode<T> leftSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index - 1),order,type);
            if (leftSibling.keys.size() > (order - 1) / 2) {
                borrowFromLeftSibling(parent, index, node, leftSibling);
            } else {
                mergeWithLeftSibling(parent, index, node, leftSibling);
            }
            serializeNode(leftSibling);
        } else if (index < parent.childrenOffsets.size() - 1) {
            BPlusTreeNode<T> rightSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index + 1),order,type);
            if (rightSibling.keys.size() > (order - 1) / 2) {
                borrowFromRightSibling(parent, index, node, rightSibling);
            } else {
                mergeWithRightSibling(parent, index, node, rightSibling);
            }
            serializeNode(rightSibling);
        }
        serializeNode(parent);
    }

    /**
     * Borrow a key and child from the left sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param leftSibling The left sibling node.
     */
    private void borrowFromLeftSibling(BPlusTreeNode<T> parent, int index, BPlusTreeNode<T> node, BPlusTreeNode<T> leftSibling) {
        if (node.isLeaf) {
            // Leaf node: borrow a key-value pair from the left sibling
            int parentKeyIndex = index - 1;

            T movingKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
            node.keys.add(0, movingKey);
            node.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
            parent.keys.set(parentKeyIndex, movingKey);
        } else {
            // Internal node: borrow a key and child from the left sibling
            int parentKeyIndex = index - 1;
            T parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(0, parentKey);
            node.childrenOffsets.add(0, leftSibling.childrenOffsets.remove(leftSibling.childrenOffsets.size() - 1));
            parent.keys.set(parentKeyIndex, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        }
    }

    /**
     * Borrow a key and child from the right sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param rightSibling The right sibling node.
     */
    private void borrowFromRightSibling(BPlusTreeNode<T> parent, int index, BPlusTreeNode<T> node, BPlusTreeNode<T> rightSibling) {
        int parentKeyIndex = index;
        if (node.isLeaf) {
            // Leaf node: borrow a key-value pair from the right sibling
            T parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.values.add(rightSibling.values.remove(0));
        } else {
            // Internal node: borrow a key and child from the right sibling
            T parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.childrenOffsets.add(rightSibling.childrenOffsets.remove(0));
        }
        parent.keys.set(parentKeyIndex, rightSibling.keys.remove(0));
    }

    /**
     * Merge a node with its left sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param leftSibling The left sibling node.
     */
    private void mergeWithLeftSibling(BPlusTreeNode<T> parent, int index, BPlusTreeNode<T> node, BPlusTreeNode<T> leftSibling) throws InvalidTypeException {
        int parentKeyIndex = index - 1; // The index of the key in the parent separating the nodes

        // Combine the current node with the left sibling
        leftSibling.keys.addAll(node.keys);
        leftSibling.childrenOffsets.addAll(node.childrenOffsets);

        // Remove the key from the parent and the current node from the parent's children list
        parent.keys.remove(parentKeyIndex);
        parent.childrenOffsets.remove(index);

        // Serialize the updated nodes
        serializeNode(leftSibling);
        serializeNode(parent);

        // Handle the case where the parent node becomes underflow
        if (parent.keys.size() < (order - 1) / 2 && parent != root) {
            handleUnderflow(parent);
        }

        // Handle the case where the parent node is empty
        if (parent.keys.isEmpty()) {
            int midIndex = (leftSibling.keys.size() - 1) / 2;
            parent.keys.add(leftSibling.keys.get(midIndex));
        }
    }

    /**
     * Merge a node with its right sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param rightSibling The right sibling node.
     */
    private void mergeWithRightSibling(BPlusTreeNode<T> parent, int index, BPlusTreeNode<T> node, BPlusTreeNode<T> rightSibling) throws InvalidTypeException {
        int parentKeyIndex = index; // The index of the parent key separating `node` and `rightSibling`

        // Merge the current node with the right sibling
        node.keys.addAll(rightSibling.keys);
        if (node.isLeaf) {
            node.values.addAll(rightSibling.values);
        } else {
            node.childrenOffsets.addAll(rightSibling.childrenOffsets);
        }

        // Remove the parent key and the right sibling from the parent’s children list
        parent.keys.remove(parentKeyIndex);
        parent.childrenOffsets.remove(index + 1);

        // Serialize the updated nodes
        serializeNode(node);
        serializeNode(parent);

        // Handle the case where parent node becomes underflow
        if (parent.keys.size() < (order - 1) / 2 && parent != root) {
            handleUnderflow(parent);
        }

        // Handle the case where the parent node is empty
        if (parent.keys.isEmpty()) {
            int midIndex = (rightSibling.keys.size() - 1) / 2;
            parent.keys.add(rightSibling.keys.get(midIndex));
        }
    }

    /**
     * Serialize a node to the ByteBuffer.
     *
     * @param node The node to serialize.
     */
    private void serializeNode(BPlusTreeNode<T> node) throws InvalidTypeException {
        buffer.position(node.offset);
        node.serialize(buffer);
        buffer.position(node.getEndOffset());
    }

    /**
     * Print the structure of the B+ Tree starting from the root node, with custom indentation.
     *
     * @param indent A string used for indentation to represent the tree's structure visually.
     *               This allows the caller to specify how deeply indented the output should be.
     */
    public void printTree(String indent) throws InvalidTypeException {
        // Clear the set of printed offsets to ensure a fresh start for printing.
        printedOffsets.clear();
        // Call the recursive printTree method to start printing from the root node.
        printTree(root, indent, null);
    }

    /**
     * Print the structure of the B+ Tree starting from the root node, with default indentation.
     */
    public void printTree() throws InvalidTypeException {
        // Clear the set of printed offsets to ensure a fresh start for printing.
        printedOffsets.clear();
        // Call the recursive printTree method to start printing from the root node.
        printTree(root, "", null);
    }

    /**
     * Print the structure of the B+ Tree.
     *
     * @param node The starting node (usually the root).
     * @param indent The indentation for tree levels.
     * @param parentOffset The offset of the parent node, used to show parent-child relationships.
     */
    private void printTree(BPlusTreeNode<T> node, String indent, Integer parentOffset) throws InvalidTypeException {
        // Check if this node has already been printed
        if (printedOffsets.contains(node.offset)) {
            return; // Skip printing if already printed
        }

        // Print node type, offset, and keys
        String nodeType = node.isLeaf ? "Leaf" : "Internal";
        System.out.println(indent + nodeType + " Node (Offset: " + node.offset + "):");
        System.out.println(indent + "  Keys: " + node.keys);
        if (node.isLeaf) {
            System.out.println(indent + "  Values: " + node.values);
        } else {
            System.out.println(indent + "  Children Offsets: " + node.childrenOffsets);
        }

        // Print the parent-child relationship if a parentOffset is provided
        if (parentOffset != null) {
            System.out.println(indent + "  Parent Offset: " + parentOffset);
        }

        // Mark this node as printed
        printedOffsets.add(node.offset);

        // Recursively print child nodes for internal nodes
        if (!node.isLeaf) {
            for (Integer offset : node.childrenOffsets) {
                BPlusTreeNode<T> child = BPlusTreeNode.deserialize(buffer, offset, order,type);
                printTree(child, indent + "  ", node.offset);
            }
        }
    }
    @Override
    public String toString() {
        try {
            printTree();
        } catch (InvalidTypeException e) {
            throw new RuntimeException(e);
        }
        // Clear the set of printed offsets to ensure a fresh start for converting the tree to a string.
        printedOffsets.clear();
        // Use StringBuilder to build the result and start recursion from the root node.
        StringBuilder sb = new StringBuilder();
        try {
            toString(root, "", null, sb);
        } catch (InvalidTypeException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    /**
     * Build the string representation of the B+ Tree.
     *
     * @param node The starting node (usually the root).
     * @param indent The indentation for tree levels.
     * @param parentOffset The offset of the parent node, used to show parent-child relationships.
     * @param sb The StringBuilder to accumulate the result.
     */
    private void toString(BPlusTreeNode<T> node, String indent, Integer parentOffset, StringBuilder sb) throws InvalidTypeException {
        // Check if this node has already been processed
        if (printedOffsets.contains(node.offset)) {
            return; // Skip processing if already processed
        }

        // Add node type, offset, and keys to the string builder
        String nodeType = node.isLeaf ? "Leaf" : "Internal";
        sb.append(indent).append(nodeType).append(" Node (Offset: ").append(node.offset).append("):\n");
        sb.append(indent).append("  Keys: ").append(node.keys).append("\n");
        if (node.isLeaf) {
            sb.append(indent).append("  Values: ").append(node.values).append("\n");
        } else {
            sb.append(indent).append("  Children Offsets: ").append(node.childrenOffsets).append("\n");
        }

        // Append the parent-child relationship if a parentOffset is provided
        if (parentOffset != null) {
            sb.append(indent).append("  Parent Offset: ").append(parentOffset).append("\n");
        }

        // Mark this node as processed
        printedOffsets.add(node.offset);

        // Recursively process child nodes for internal nodes
        if (!node.isLeaf) {
            for (Integer offset : node.childrenOffsets) {
                BPlusTreeNode<T> child = BPlusTreeNode.deserialize(buffer, offset, order,type);
                toString(child, indent + "  ", node.offset, sb);
            }
        }
    }

}
