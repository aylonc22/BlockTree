package org.example.P2P;

import java.util.ArrayList;
import java.util.List;

public class PeerManager {
    private List<String> peers = new ArrayList<>();

    public void addPeer(String peerAddress) {
        if (!peers.contains(peerAddress)) {
            peers.add(peerAddress);
        }
    }

    public List<String> getPeers() {
        return peers;
    }
}

