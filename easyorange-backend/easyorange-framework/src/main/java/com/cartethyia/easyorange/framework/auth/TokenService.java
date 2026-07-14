package com.cartethyia.easyorange.framework.auth;

import java.util.Collection;

public interface TokenService {

    String createAccessToken(String userId, String username, Collection<String> authorities);

    String createRefreshToken(String userId, String username, Collection<String> authorities);

    void invalidateToken(String token);

    void invalidateAllUserTokens(String userId);

    TokenRefreshResult refreshToken(String refreshToken);
}
