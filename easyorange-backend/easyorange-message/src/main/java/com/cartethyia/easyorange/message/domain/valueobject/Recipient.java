package com.cartethyia.easyorange.message.domain.valueobject;

public record Recipient(Long userId, String channel) {

    public Recipient {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (channel == null || channel.isBlank()) {
            channel = "websocket";
        }
    }

    public static Recipient websocket(Long userId) {
        return new Recipient(userId, "websocket");
    }
}
