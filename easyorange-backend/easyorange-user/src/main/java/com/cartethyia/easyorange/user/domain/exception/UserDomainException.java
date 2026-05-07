package com.cartethyia.easyorange.user.domain.exception;

public class UserDomainException extends RuntimeException {

    public UserDomainException(String message) {
        super(message);
    }

    public UserDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
