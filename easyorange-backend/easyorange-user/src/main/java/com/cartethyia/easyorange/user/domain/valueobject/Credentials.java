package com.cartethyia.easyorange.user.domain.valueobject;

import java.util.Objects;

public record Credentials(String username, String encodedPassword) {
    public Credentials {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(encodedPassword, "password must not be null");
    }

    public Credentials changePassword(String newEncodedPassword) {
        return new Credentials(username, newEncodedPassword);
    }
}
