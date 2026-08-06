package com.cartethyia.easyorange.message.domain.valueobject;

public record UserInfo(String id, String username, String avatar) {
    public static UserInfo of(String id, String username, String avatar) {
        return new UserInfo(id, username, avatar);
    }
}
