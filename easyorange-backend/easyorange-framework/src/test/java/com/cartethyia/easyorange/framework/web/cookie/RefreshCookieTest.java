package com.cartethyia.easyorange.framework.web.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

/**
 * RefreshToken HttpOnly Cookie 装配 — 单元测试。
 * <p>
 * 验证：write 生成 HttpOnly+Secure+SameSite+Path 受限的 Set-Cookie（maxAge = refresh 生命周期）
 * + 非 HttpOnly 标记 has_rt（TD-023：前端冷启动据此预判有无 refresh 会话）；
 * clear 让两者一并过期。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Refresh HttpOnly Cookie 装配")
class RefreshCookieTest {

    private final JwtProperties jwtProperties = PropertyBindings.bind(JwtProperties.class);

    @Mock
    private HttpServletResponse response;

    private RefreshCookie refreshCookie;

    @BeforeEach
    void setUp() {
        refreshCookie = new RefreshCookie(jwtProperties);
    }

    @Test
    @DisplayName("write 生成 HttpOnly/Secure/SameSite/Path 受限的 Set-Cookie")
    void write_buildsHttpOnlySecureSameSiteCookie() {
        refreshCookie.write(response, "opaque-refresh-token");

        var headers = captureSetCookieHeaders();
        var header = headers.get(0);
        assertThat(header).contains("eo_refresh_token=opaque-refresh-token");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("SameSite=Lax");
        assertThat(header).contains("Path=/api/auth");
        assertThat(header).contains("Max-Age=" + (7L * 24 * 3600));
    }

    @Test
    @DisplayName("write 同发非 HttpOnly 标记 has_rt（path=/ 供前端预判，TD-023）")
    void write_setsReadableMarkerCookie() {
        refreshCookie.write(response, "opaque-refresh-token");

        var headers = captureSetCookieHeaders();
        var marker = headers.get(1);
        assertThat(marker).contains(RefreshCookie.MARKER_COOKIE_NAME + "=1");
        assertThat(marker).contains("Path=/");
        assertThat(marker).doesNotContain("HttpOnly");
        assertThat(marker).contains("Max-Age=" + (7L * 24 * 3600));
    }

    @Test
    @DisplayName("clear 生成 maxAge=0 的过期 Cookie（refresh 与标记一并清）")
    void clear_buildsExpiringCookie() {
        refreshCookie.clear(response);

        var headers = captureSetCookieHeaders();
        assertThat(headers.get(0)).contains("eo_refresh_token=");
        assertThat(headers.get(0)).contains("Max-Age=0");
        assertThat(headers.get(0)).contains("HttpOnly");
        assertThat(headers.get(1)).contains(RefreshCookie.MARKER_COOKIE_NAME + "=");
        assertThat(headers.get(1)).contains("Max-Age=0");
    }

    private List<String> captureSetCookieHeaders() {
        var captor = ArgumentCaptor.forClass(String.class);
        verify(response, atLeastOnce()).addHeader(eq(HttpHeaders.SET_COOKIE), captor.capture());
        return captor.getAllValues();
    }
}
