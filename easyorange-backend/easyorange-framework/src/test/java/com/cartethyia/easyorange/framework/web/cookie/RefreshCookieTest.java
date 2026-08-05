package com.cartethyia.easyorange.framework.web.cookie;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RefreshToken HttpOnly Cookie 装配 — 单元测试。
 * <p>
 * 验证：write 生成 HttpOnly+Secure+SameSite+Path 受限的 Set-Cookie，maxAge 等于 refresh 生命周期；
 * clear 生成 maxAge=0 的过期 Cookie。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Refresh HttpOnly Cookie 装配")
class RefreshCookieTest {

    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private HttpServletResponse response;

    private RefreshCookie refreshCookie;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getRefreshCookieName()).thenReturn("eo_refresh_token");
        when(jwtProperties.getRefreshCookiePath()).thenReturn("/api/auth");
        when(jwtProperties.isRefreshCookieSecure()).thenReturn(true);
        when(jwtProperties.getRefreshCookieSameSite()).thenReturn("Lax");
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(7L);
        refreshCookie = new RefreshCookie(jwtProperties);
    }

    @Test
    @DisplayName("write 生成 HttpOnly/Secure/SameSite/Path 受限的 Set-Cookie")
    void write_buildsHttpOnlySecureSameSiteCookie() {
        refreshCookie.write(response, "opaque-refresh-token");

        var header = captureSetCookie();
        assertThat(header).contains("eo_refresh_token=opaque-refresh-token");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("SameSite=Lax");
        assertThat(header).contains("Path=/api/auth");
        assertThat(header).contains("Max-Age=" + (7L * 24 * 3600));
    }

    @Test
    @DisplayName("clear 生成 maxAge=0 的过期 Cookie")
    void clear_buildsExpiringCookie() {
        refreshCookie.clear(response);

        var header = captureSetCookie();
        assertThat(header).contains("eo_refresh_token=");
        assertThat(header).contains("Max-Age=0");
        assertThat(header).contains("HttpOnly");
    }

    private String captureSetCookie() {
        var captor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), captor.capture());
        return captor.getValue();
    }
}