package com.cartethyia.easyorange.framework.auth;

public interface TokenService {

    String createAccessToken(String userId, String username, String userType);

    String createRefreshToken(String userId, String username, String userType);

    void invalidateToken(String token);

    void invalidateAllUserTokens(String userId);

    TokenRefreshResult refreshToken(String refreshToken);
}
