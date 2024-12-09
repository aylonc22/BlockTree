import com.fasterxml.jackson.databind.ObjectMapper;

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
