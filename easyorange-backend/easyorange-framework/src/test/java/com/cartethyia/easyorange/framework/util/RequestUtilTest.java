package com.cartethyia.easyorange.framework.util;

import jakarta.servlet.http.HttpServletRequest;
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

    @Nested
    @DisplayName("getClientIp with HttpServletRequest")
    class GetClientIpWithRequestTests {

        @Test
        @DisplayName("should return remote addr when no proxy headers present")
        void getClientIp_withoutProxyHeaders_shouldReturnRemoteAddr() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should return X-Forwarded-For first IP")
        void getClientIp_withXForwardedFor_shouldReturnFirstIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should return X-Real-IP when X-Forwarded-For absent")
        void getClientIp_withXRealIp_shouldReturnIt() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("203.0.113.2");
        }

        @Test
        @DisplayName("should handle multiple comma-separated IPs")
        void getClientIp_withMultipleProxies_shouldReturnFirst() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("should convert IPv6 localhost (full form) to IPv4")
        void getClientIp_withIPv6LocalhostFull_shouldConvertToIPv4() {
            when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("should convert IPv6 localhost (short form) to IPv4")
        void getClientIp_withIPv6LocalhostShort_shouldConvertToIPv4() {
            when(request.getRemoteAddr()).thenReturn("::1");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("should convert IPv4-mapped IPv6 localhost to IPv4")
        void getClientIp_withIPv4MappedIPv6Localhost_shouldConvertToIPv4() {
            when(request.getRemoteAddr()).thenReturn("::ffff:127.0.0.1");

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
        @DisplayName("should fallback to remote addr when proxy header is unknown")
        void getClientIp_withUnknownProxyHeader_shouldFallbackToRemoteAddr() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("should fallback to remote addr when all proxy headers are null")
        void getClientIp_withNullProxyHeaders_shouldFallback() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);

            String ip = RequestUtil.getClientIp(request);

            assertThat(ip).isEqualTo("192.168.1.1");
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
            String path = RequestUtil.getRequestPath();

            assertThat(path).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("getClientIp without arguments")
    class GetClientIpNoArgsTests {

        @Test
        @DisplayName("should return UNKNOWN when no request attributes")
        void getClientIp_withNoContext_shouldReturnUnknown() {
            String ip = RequestUtil.getClientIp();

            assertThat(ip).isEqualTo("unknown");
        }
    }
}
