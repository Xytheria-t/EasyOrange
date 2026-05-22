package com.cartethyia.easyorange.message.domain.valueobject;

import com.cartethyia.easyorange.message.domain.valueobject.MessageContentFormat;

public record MessageContent(String value, MessageContentFormat type) {

    public MessageContent {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (value.length() > 5000) {
            throw new IllegalArgumentException("Message content too long");
        }
        if (type == null) {
            type = MessageContentFormat.TEXT;
        }
    }

    public static MessageContent text(String content) {
        return new MessageContent(content, MessageContentFormat.TEXT);
    }

    public static MessageContent markdown(String content) {
        return new MessageContent(content, MessageContentFormat.MARKDOWN);
    }
}
