package com.cartethyia.easyorange.framework.auth;

public interface TokenService {

    String createAccessToken(Long userId, String username, String userType);

    String createRefreshToken(Long userId, String username, String userType);

    void invalidateToken(String token);

    void invalidateAllUserTokens(Long userId);

    TokenRefreshResult refreshToken(String refreshToken);
}
