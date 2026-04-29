package com.cartethyia.easyorange.framework.service;

public interface TokenService {

    String createAccessToken(Long userId, String username, String userType);

    String createRefreshToken(Long userId, String username, String userType);

    void delToken(String token);

    void revokeAllTokens(String accessToken, String refreshToken);

    Long verifyTokenAndGetUserId(String token);

    String refreshToken(String refreshToken);
}
