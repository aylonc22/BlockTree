package org.example.BlockChain;

import org.example.BPlusTree.BPlusTree;
import org.example.Transaction.Transaction;

import java.time.Instant;

public class Block {
    private String previousBlockHash;  // Link to the previous block
    private String blockHash;          // Unique hash of the block
    private long timestamp;            // Block creation timestamp
    private BPlusTree transactions;       // B+ Tree to store key-value pairs in the block
    public static final int MAX_BLOCK_SIZE = 1; // 1MB

    // Constructor
    public Block(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
        this.timestamp = Instant.now().getEpochSecond();
        this.transactions = new BPlusTree(MAX_BLOCK_SIZE,3);
        this.blockHash = calculateBlockHash();  // Calculate the block's hash
    }

    // Method to calculate the block's hash based on the previous block's hash and timestamp
    public String calculateBlockHash() {
        String dataToHash = previousBlockHash + timestamp + transactions.hashCode();
        return String.valueOf(dataToHash.hashCode());
    }
    public void addTransaction(Transaction transaction){
        this.transactions.insert(transaction.transactionHash,transaction.toJson());
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

    public BPlusTree getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "Block{" +
                "previousBlockHash='" + previousBlockHash + '\'' +
                ", blockHash='" + blockHash + '\'' +
                ", timestamp=" + timestamp +
                ", transactions=" + transactions.toString() +
                '}';
    }
}

