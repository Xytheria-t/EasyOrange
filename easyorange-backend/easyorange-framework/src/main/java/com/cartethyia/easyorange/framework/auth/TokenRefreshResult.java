package com.cartethyia.easyorange.framework.auth;

public record TokenRefreshResult(
    String accessToken,
    String refreshToken
) {}
