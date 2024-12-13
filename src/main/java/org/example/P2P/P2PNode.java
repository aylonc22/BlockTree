package org.example.P2P;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class P2PNode {
    private static final int PORT = 5000;
    private static ServerSocket serverSocket;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                // Accept incoming connections from other peers
                Socket clientSocket = serverSocket.accept();
                pool.submit(new ClientHandler(clientSocket)); // Handle communication in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handler to manage the communication between peers
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                String message;

                // Read incoming messages from peer
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    // Respond with a simple acknowledgment (or implement more complex logic here)
                    out.println("Message received: " + message);
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

