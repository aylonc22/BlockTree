package org.example;

import org.example.BlockChain.Block;

import java.security.NoSuchAlgorithmException;

public class Miner {
    private static final String DIFFICULTY_PREFIX = "0000";  // Difficulty level (adjust based on needs)

    public static String mine(Block block) throws NoSuchAlgorithmException {
        // Increment the nonce until the hash satisfies the difficulty
        while (!block.getBlockHash().startsWith(DIFFICULTY_PREFIX)) {
            block.setBlockHash(block.calculateBlockHash());
        }
        return block.getBlockHash();  // Return the valid hash
    }
}
