package com.cartethyia.easyorange.framework.auth;

/**
 * 刷新令牌响应：仅返回新 access token（refresh 经 HttpOnly Cookie 下发）。
 */
public record TokenRefreshResult(String accessToken) {}
