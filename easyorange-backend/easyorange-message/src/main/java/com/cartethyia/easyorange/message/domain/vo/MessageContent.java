package com.cartethyia.easyorange.message.domain.vo;

public record MessageContent(String value, MessageType type) {

    public MessageContent {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (value.length() > 5000) {
            throw new IllegalArgumentException("Message content too long");
        }
        if (type == null) {
            type = MessageType.TEXT;
        }
    }

    public static MessageContent text(String content) {
        return new MessageContent(content, MessageType.TEXT);
    }

    public static MessageContent markdown(String content) {
        return new MessageContent(content, MessageType.MARKDOWN);
    }
}
