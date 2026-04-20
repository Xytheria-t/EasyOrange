package com.cartethyia.easyorange.user.service;

public interface LoginSecurityService {

    void checkLoginAttempts(String account);

    void recordFailedAttempt(String account);

    void clearLoginAttempts(String account);

    String maskAccount(String account);
}
