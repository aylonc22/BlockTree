package org.example.BlockChain;
import com.sun.jdi.InvalidTypeException;
import org.example.MessageProtocol.Message;
import org.example.Miner;
import org.example.P2P.P2PNode;
import org.example.P2P.PeerManager;
import org.example.Transaction.Transaction;
import org.example.Transaction.TransactionValidator;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BlockChainNode {
    private static final int PORT = 5000;
    private static BlockChain blockchain;

    static {
        try {
            blockchain = new BlockChain();
        } catch (InvalidTypeException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static PeerManager peerManager = new PeerManager();

    public static void main(String[] args) {
        try {
            // Start listening for incoming connections
            P2PNode p2pNode = new P2PNode();
           Thread p2pThread = new Thread(p2pNode);
            p2pThread.start();

            // Add a bootstrap peer (for simplicity, we manually add one peer)
            peerManager.addPeer(peerManager.getBootstrapPeer());

            // Simulate adding transactions
            Transaction transaction = new Transaction("Alice", "Bob", 10,System.currentTimeMillis());
            if (TransactionValidator.validateTransaction(transaction)) {
                blockchain.addTransaction(transaction);
            }

            // Start mining and adding blocks to the blockchain
            Block block = blockchain.createBlock();
            String minedHash = Miner.mine(block);
            blockchain.addBlock(block);

            // Broadcast the new block to peers
            String blockMessage = new Message(Message.MessageType.NEW_BLOCK, minedHash).toString();
            for (String peer : peerManager.getPeers()) {
                sendMessageToPeer(peer, blockMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | InvalidTypeException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessageToPeer(String peerAddress, String message) {
        try {
            Socket socket = new Socket(peerAddress.split(":")[0], Integer.parseInt(peerAddress.split(":")[1]));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
