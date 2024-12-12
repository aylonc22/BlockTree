package org.example.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Util.HashUtil;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Transaction {
    public String sender;
    public String recipient;
    public double amount;
    public long timestamp;
    public String transactionHash;

    public Transaction(String sender, String recipient, double amount, long timestamp) throws NoSuchAlgorithmException {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.timestamp = timestamp;
        this.transactionHash = generateTransactionHash();
    }

    private String generateTransactionHash() throws NoSuchAlgorithmException {
        // Generate a unique nonce (random value)
        String nonce = UUID.randomUUID().toString();

        // Create a transaction string with the original fields + the nonce
        String transactionData = sender + recipient + amount + timestamp + nonce;

        // Compute the hash using SHA-256
        return HashUtil.computeSha256(transactionData);
    }

    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize transaction to JSON", e);
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", transactionHash='" + transactionHash + '\'' +
                '}';
    }
}
