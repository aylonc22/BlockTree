package org.example.BlockChain;

import com.sun.jdi.InvalidTypeException;
import org.example.BPlusTree.BPlusTree;
import org.example.Transaction.Transaction;
import org.example.Util.HashUtil;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public class Block {
    private String previousBlockHash;  // Link to the previous block
    private String blockHash;          // Unique hash of the block
    private long timestamp;            // Block creation timestamp
    private BPlusTree<String> transactions;       // B+ Tree to store key-value pairs in the block
    public static final int MAX_BLOCK_SIZE = 1; // 1MB

    // Constructor
    public Block(String previousBlockHash) throws InvalidTypeException, NoSuchAlgorithmException {
        this.previousBlockHash = previousBlockHash;
        this.timestamp = Instant.now().getEpochSecond();
        this.transactions = new BPlusTree<>(MAX_BLOCK_SIZE,3,String.class);
        this.blockHash = calculateBlockHash();  // Calculate the block's hash
    }

    // Method to calculate the block's hash based on the previous block's hash and timestamp
    public String calculateBlockHash() throws NoSuchAlgorithmException {
        String dataToHash = previousBlockHash + timestamp + transactions.generateHashCode();
        return HashUtil.computeSha256(String.valueOf(dataToHash.hashCode()));
    }
    public void addTransaction(Transaction transaction) throws InvalidTypeException {
        this.transactions.insert(String.valueOf(transaction.transactionHash),transaction.toJson());
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

