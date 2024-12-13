package org.example.MessageProtocol;

public class Message {
    public enum MessageType {
        NEW_TRANSACTION, NEW_BLOCK, GET_BLOCKS
    }

    public MessageType type;
    public String data;

    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }
}

