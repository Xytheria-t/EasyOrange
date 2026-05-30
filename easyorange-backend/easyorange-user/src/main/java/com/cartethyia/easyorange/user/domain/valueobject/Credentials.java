package com.cartethyia.easyorange.user.domain.valueobject;

import java.util.Objects;

public record Credentials(String username, String encodedPassword) {
    public Credentials {
        Objects.requireNonNull(username, "用户名不能为空");
        Objects.requireNonNull(encodedPassword, "密码不能为空");
    }

    public Credentials changePassword(String newEncodedPassword) {
        return new Credentials(username, newEncodedPassword);
    }
}
