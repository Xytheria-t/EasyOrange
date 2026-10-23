package com.cartethyia.easyorange.message.domain.valueobject;

public record MessageUserProfile(String id, String username, String avatar) {
    public static MessageUserProfile of(String id, String username, String avatar) {
        return new MessageUserProfile(id, username, avatar);
    }
}
