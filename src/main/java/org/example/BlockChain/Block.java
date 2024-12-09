package org.example.BlockChain;

import org.example.BPlusTree.BPlusTree;

import java.time.Instant;

public class Block {
    private String previousBlockHash;  // Link to the previous block
    private String blockHash;          // Unique hash of the block
    private long timestamp;            // Block creation timestamp
    private BPlusTree bPlusTree;       // B+ Tree to store key-value pairs in the block

    // Constructor
    public Block(String previousBlockHash, BPlusTree bPlusTree) {
        this.previousBlockHash = previousBlockHash;
        this.timestamp = Instant.now().getEpochSecond();
        this.bPlusTree = bPlusTree;
        this.blockHash = calculateBlockHash();  // Calculate the block's hash
    }

    // Method to calculate the block's hash based on the previous block's hash and timestamp
    public String calculateBlockHash() {
        String dataToHash = previousBlockHash + timestamp + bPlusTree.hashCode();
        return String.valueOf(dataToHash.hashCode());
    }

    // Getters
    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public BPlusTree getBPlusTree() {
        return bPlusTree;
    }
}

