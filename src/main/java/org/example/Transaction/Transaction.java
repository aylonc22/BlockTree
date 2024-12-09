package org.example.Transaction;

public class Transaction {
    public String sender;
    public String recipient;
    public double amount;
    public long timestamp;
    public String transactionHash;

    public Transaction(String sender, String recipient, double amount, long timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.timestamp = timestamp;
        this.transactionHash = generateTransactionHash();
    }

    private String generateTransactionHash() {
        return String.format("%x", (sender + recipient + amount + timestamp).hashCode());
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

