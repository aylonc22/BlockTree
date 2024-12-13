package org.example.Transaction;

public class TransactionValidator {
    public static boolean validateTransaction(Transaction transaction) {
        // For simplicity, assume basic validation:
        return transaction.sender != null && transaction.recipient != null && transaction.amount > 0;
    }
}

