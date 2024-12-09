package org.example.BlockChain;
import org.example.BPlusTree.BPlusTree;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    private List<Block> chain;  // List to store the blocks in the blockchain

    // Constructor
    public BlockChain() {
        chain = new ArrayList<>();
        // Initialize the blockchain with the genesis block (first block)
        Block genesisBlock = createGenesisBlock();
        chain.add(genesisBlock);
    }

    // Method to create the genesis block (the first block)
    private Block createGenesisBlock() {
        // The previous block hash for the genesis block is typically "0" or null
        BPlusTree genesisTree = new BPlusTree();
        return new Block("0", genesisTree);
    }

    // Method to add a block to the blockchain
    public void addBlock(BPlusTree bPlusTree) {
        Block previousBlock = chain.get(chain.size() - 1);
        String previousBlockHash = previousBlock.getBlockHash();
        Block newBlock = new Block(previousBlockHash, bPlusTree);
        chain.add(newBlock);
    }

    // Method to get the latest block in the blockchain
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    // Method to validate the blockchain (ensure integrity)
    public boolean validateBlockchain() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Check if the previous block hash matches the previous block's hash
            if (!currentBlock.getPreviousBlockHash().equals(previousBlock.getBlockHash())) {
                return false; // Blockchain is invalid
            }

            // Check if the current block's hash matches its calculated hash
            if (!currentBlock.getBlockHash().equals(currentBlock.calculateBlockHash())) {
                return false; // Blockchain is invalid
            }
        }
        return true; // Blockchain is valid
    }

    // Method to retrieve a block by its hash (could be useful for querying)
    public Block getBlockByHash(String blockHash) {
        for (Block block : chain) {
            if (block.getBlockHash().equals(blockHash)) {
                return block;
            }
        }
        return null; // Block not found
    }

    // Method to print the blockchain (for debugging purposes)
    public void printBlockchain() {
        for (Block block : chain) {
            System.out.println("Block Hash: " + block.getBlockHash());
            System.out.println("Previous Block Hash: " + block.getPreviousBlockHash());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("B+ Tree: " + block.getBPlusTree());  // Print the B+ Tree stored in the block
            System.out.println("--------------------------------------");
        }
    }
}


