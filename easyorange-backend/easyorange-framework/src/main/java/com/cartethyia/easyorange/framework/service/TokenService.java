package com.cartethyia.easyorange.framework.service;

public interface TokenService {

    String createAccessToken(Long userId, String username, String userType);

    void delToken(String token);

    Long verifyTokenAndGetUserId(String token);

    String refreshToken(String token);
}
