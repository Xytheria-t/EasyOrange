package com.cartethyia.easyorange.framework.service;

public interface TokenService {

    String createToken(Long userId, String username, String userType);

    String createToken(Long userId, String username);

    @Deprecated(since = "use verifyTokenAndGetUserId instead")
    boolean verifyToken(String token);

    Long getUserId(String token);

    void delToken(String token);

    Long verifyTokenAndGetUserId(String token);

    String refreshToken(String token);
}
