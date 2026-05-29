package com.cartethyia.easyorange.framework.service;

public record TokenRefreshResult(
    String accessToken,
    String refreshToken
) {}
