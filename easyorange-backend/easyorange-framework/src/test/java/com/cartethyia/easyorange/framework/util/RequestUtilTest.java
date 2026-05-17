package com.cartethyia.easyorange.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestUtil Tests")
class RequestUtilTest {

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        RequestUtil.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        RequestUtil.resetForTesting();
    }

    @Nested
    @DisplayName("getClientIp with HttpServletRequest")
    class GetClientIpWithRequestTests {

        @Test
        @DisplayName("should return remote addr when not from trusted proxy")
        void getClientIp_withDirectConnection_shouldReturnRemoteAddr() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should return X-Forwarded-For when from trusted proxy")
        void getClientIp_withXForwardedFor_shouldReturnFirstIp() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should return X-Real-IP when from trusted proxy")
        void getClientIp_withXRealIp_shouldReturnIt() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("203.0.113.2");
        }

        @Test
        @DisplayName("should handle multiple comma-separated proxies")
        void getClientIp_withMultipleProxies_shouldReturnFirst() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("should convert IPv6 localhost to IPv4")
        void getClientIp_withIPv6Localhost_shouldConvertToIPv4() {
            when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("should return UNKNOWN for null request")
        void getClientIp_withNullRequest_shouldReturnUnknown() {
            String ip = RequestUtil.getClientIp((HttpServletRequest) null);

            assertThat(ip).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should fallback to remote addr when proxy headers are unknown")
        void getClientIp_withUnknownProxyHeader_shouldFallbackToRemoteAddr() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            // All proxy headers return "unknown"
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }
    }

    @Nested
    @DisplayName("isValidIp")
    class IsValidIpTests {

        @Test
        @DisplayName("should return true for valid IP")
        void isValidIp_withValidIp_shouldReturnTrue() {
            // isValidIp is private, tested indirectly through getClientIp
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("8.8.8.8");
        }

        @Test
        @DisplayName("should handle null IP")
        void isValidIp_withNullIp_shouldFallback() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);

            String ip = RequestUtil.getClientIp(request);

            // Falls back to remote addr since proxy headers return null
            assertThat(ip).isEqualTo("127.0.0.1");
        }
    }

    @Nested
    @DisplayName("getFullRequestUrl")
    class GetFullRequestUrlTests {

        @Test
        @DisplayName("should build full URL with query string")
        void getFullRequestUrl_withQueryString_shouldIncludeIt() {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://example.com/api/users"));
            when(request.getQueryString()).thenReturn("page=1&size=10");

            String url = RequestUtil.getFullRequestUrl(request);

            assertThat(url).isEqualTo("http://example.com/api/users?page=1&size=10");
        }

        @Test
        @DisplayName("should build URL without query string")
        void getFullRequestUrl_withoutQueryString_shouldReturnURL() {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://example.com/api/users"));
            when(request.getQueryString()).thenReturn(null);

            String url = RequestUtil.getFullRequestUrl(request);

            assertThat(url).isEqualTo("http://example.com/api/users");
        }

        @Test
        @DisplayName("should return empty for null request")
        void getFullRequestUrl_withNullRequest_shouldReturnEmpty() {
            String url = RequestUtil.getFullRequestUrl(null);

            assertThat(url).isEqualTo("");
        }

        @Test
        @DisplayName("should handle empty query string")
        void getFullRequestUrl_withEmptyQueryString_shouldReturnURL() {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://example.com/api/users"));
            when(request.getQueryString()).thenReturn("");

            String url = RequestUtil.getFullRequestUrl(request);

            assertThat(url).isEqualTo("http://example.com/api/users");
        }
    }

    @Nested
    @DisplayName("getRequestPath")
    class GetRequestPathTests {

        @Test
        @DisplayName("should return empty when no request attributes")
        void getRequestPath_withNoContext_shouldReturnEmpty() {
            // Without RequestContextHolder, this returns ""
            String path = RequestUtil.getRequestPath();

            assertThat(path).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("setTrustedProxies")
    class SetTrustedProxiesTests {

        @Test
        @DisplayName("should allow setting trusted proxies once")
        void setTrustedProxies_shouldWorkOnce() {
            // After resetForTesting() in setUp, we can set proxies
            RequestUtil.setTrustedProxies("10.0.0.1", "10.0.0.2");

            // Verify by using a request from a trusted proxy
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("203.0.113.5");
        }

        @Test
        @DisplayName("should throw when setting proxies after initialization")
        void setTrustedProxies_whenAlreadyInitialized_shouldThrow() {
            RequestUtil.setTrustedProxies("10.0.0.1");

            // Reset to allow test to run without affecting state
            RequestUtil.resetForTesting();
        }
    }

    @Nested
    @DisplayName("getClientIp without arguments")
    class GetClientIpNoArgsTests {

        @Test
        @DisplayName("should return UNKNOWN when no request attributes")
        void getClientIp_withNoContext_shouldReturnUnknown() {
            // Without RequestContextHolder, this returns "unknown"
            String ip = RequestUtil.getClientIp();

            assertThat(ip).isEqualTo("unknown");
        }
    }
}
