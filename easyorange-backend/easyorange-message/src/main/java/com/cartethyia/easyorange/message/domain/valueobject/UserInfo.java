package com.cartethyia.easyorange.message.domain.valueobject;

public record UserInfo(
    Long id,
    String username,
    String avatar
) {
    public static UserInfo of(Long id, String username, String avatar) {
        return new UserInfo(id, username, avatar);
    }
}
