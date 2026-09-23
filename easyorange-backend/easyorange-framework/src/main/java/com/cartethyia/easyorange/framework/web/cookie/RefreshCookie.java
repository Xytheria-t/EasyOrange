package com.cartethyia.easyorange.framework.web.cookie;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 的 HttpOnly Cookie 装配。
 * <p>
 * Cookie 带 HttpOnly + Secure + SameSite + Path 受限（默认 /api/auth），JS 不可见；
 * maxAge 等于 refresh 生命周期。所有属性来自 {@link JwtProperties}（配置驱动）。
 * <p>
 * 同时下发非 HttpOnly 标记 cookie {@code has_rt}（path=/，随 logout 一起清）：
 * HttpOnly 不可读，前端冷启动只能盲调 /auth/refresh，无会话时必 401（TD-023 的 console 噪音）；
 * 有标记才发起刷新，无标记直接跳过。
 */
@Component
@RequiredArgsConstructor
public class RefreshCookie {

    /** 前端可读的 refresh 会话存在标记（非 HttpOnly），读取方在 session.ts。 */
    public static final String MARKER_COOKIE_NAME = "has_rt";

    private final JwtProperties jwtProperties;

    public void write(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds =
                Duration.ofDays(jwtProperties.refreshTokenExpiration()).getSeconds();
        response.addHeader(
                HttpHeaders.SET_COOKIE, build(refreshToken, maxAgeSeconds).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildMarker(maxAgeSeconds).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildMarker(0).toString());
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(jwtProperties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite().attributeValue())
                .path(jwtProperties.refreshCookiePath())
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private ResponseCookie buildMarker(long maxAgeSeconds) {
        // path=/ 而非 refresh 的受限 path：页面在任意路由都要能读到标记
        return ResponseCookie.from(MARKER_COOKIE_NAME, "1")
                .httpOnly(false)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite().attributeValue())
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
