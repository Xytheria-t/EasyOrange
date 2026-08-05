package com.cartethyia.easyorange.framework.web.cookie;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 的 HttpOnly Cookie 装配。
 * <p>
 * Cookie 带 HttpOnly + Secure + SameSite + Path 受限（默认 /api/auth），JS 不可见；
 * maxAge 等于 refresh 生命周期。所有属性来自 {@link JwtProperties}（配置驱动）。
 */
@Component
@RequiredArgsConstructor
public class RefreshCookie {

    private final JwtProperties jwtProperties;

    public void write(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = Duration.ofDays(jwtProperties.getRefreshTokenExpiration()).getSeconds();
        response.addHeader(HttpHeaders.SET_COOKIE, build(refreshToken, maxAgeSeconds).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}