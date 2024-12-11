package org.example.Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Computes the SHA-256 hash of a given input string.
     *
     * @param input The input string to be hashed.
     * @return The SHA-256 hash of the input string, represented as a hexadecimal string.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
     */
    public static String computeSha256(String input) throws NoSuchAlgorithmException {
        // Create a MessageDigest instance for SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Apply the hash to the input string and get the byte array
        byte[] hashBytes = digest.digest(input.getBytes());

        // Convert the byte array to a hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            // Convert each byte to a two-digit hexadecimal value
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        // Return the resulting hexadecimal string
        return hexString.toString();
    }
}
