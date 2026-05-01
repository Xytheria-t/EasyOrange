package com.cartethyia.easyorange.user.domain.port;

public interface LoginAttemptPort {

    Long getAttempts(String account);

    long incrementAttempts(String account);

    void expireAttempts(String account, long minutes);

    void clearAttempts(String account);
}
