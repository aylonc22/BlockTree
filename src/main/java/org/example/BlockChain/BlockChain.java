package org.example.BlockChain;
import com.sun.jdi.InvalidTypeException;
import org.example.BPlusTree.BPlusTree;
import org.example.Transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {

    private List<Block> chain;

    public BlockChain() throws InvalidTypeException {
        chain = new ArrayList<>();
        chain.add(createGenesisBlock());
    }

    private Block createGenesisBlock() throws InvalidTypeException {
        // The first block (genesis block) doesn't have any previous hash
        return new Block(null);
    }

    // Check if the current block is full
    private boolean isCurrentBlockFull() {
        Block currentBlock = chain.get(chain.size() - 1);
        return currentBlock.getTransactions().lastAllocatedEndOffset >= (1024 * 1024) * Block.MAX_BLOCK_SIZE;
    }

    // Add a transaction to the blockchain
    public void addTransaction(Transaction transaction) throws InvalidTypeException {
        // Check if the current block is full
        if (isCurrentBlockFull()) {
            createNewBlock();  // Create a new block if the current one is full
        }

        // Add the transaction to the B+ Tree of the last block
        Block lastBlock = chain.get(chain.size() - 1);
        lastBlock.addTransaction(transaction);
    }

    // Create a new block and add it to the blockchain
    private void createNewBlock() throws InvalidTypeException {
        Block previousBlock = chain.get(chain.size() - 1);
        Block newBlock = new Block(previousBlock.getBlockHash());
        chain.add(newBlock);
    }

    @Override
    public String toString() {
        return "Blockchain{" +
                "chain=" + chain +
                '}';
    }
}



