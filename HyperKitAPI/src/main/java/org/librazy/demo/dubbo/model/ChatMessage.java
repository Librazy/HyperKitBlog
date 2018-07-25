package org.librazy.demo.dubbo.model;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1744430114257688240L;

    private MessageType type = MessageType.TEXT;

    private String content = "";

    private Long sender;

    private Long timestamp = new Date().getTime();

    private Long mid;

    protected ChatMessage() {
    }

    public ChatMessage(Long mid, MessageType type) {
        this.type = type;
        this.mid = mid;
    }

    public Long getMid() {
        return mid;
    }

    public ChatMessage setMid(Long mid) {
        this.mid = mid;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public ChatMessage setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public MessageType getType() {
        return type;
    }

    public ChatMessage setType(MessageType type) {
        this.type = type;
        return this;
    }

    public String getContent() {
        return content;
    }

    public ChatMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public Long getSender() {
        return sender;
    }

    public ChatMessage setSender(Long sender) {
        this.sender = sender;
        return this;
    }

    public enum MessageType {
        TEXT,
        MARKDOWN,
        PICTURE,
        EXTENSION,
        ENCRYPTED
    }
}