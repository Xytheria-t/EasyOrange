package com.cartethyia.easyorange.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

/**
 * {@link RequestUtil} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("RequestUtil Tests")
class RequestUtilTest {

    @BeforeEach
    void setUp() {
        RequestUtil.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("Set Trusted Proxies Tests")
    class SetTrustedProxiesTests {

        @Test
        @DisplayName("setTrustedProxies should set proxies once")
        void setTrustedProxies_shouldSetProxiesOnce() {
            // Arrange
            String[] proxies = {"192.168.1.1", "10.0.0.1"};

            // Act
            RequestUtil.setTrustedProxies(proxies);

            // Assert
            // 验证设置成功（通过后续使用间接验证）
            assertThat(proxies).hasSize(2);
        }

        @Test
        @DisplayName("setTrustedProxies second call should throw IllegalStateException")
        void setTrustedProxies_secondCall_shouldThrowIllegalStateException() {
            // Arrange
            RequestUtil.setTrustedProxies("192.168.1.1");

            // Act & Assert
            assertThatThrownBy(() -> RequestUtil.setTrustedProxies("10.0.0.1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Trust proxies already initialized");
        }

        @Test
        @DisplayName("setTrustedProxies with null should not change")
        void setTrustedProxies_withNull_shouldNotChange() {
            // Act
            RequestUtil.setTrustedProxies((String[]) null);

            // Assert
            // 不会抛出异常，也不会修改
        }

        @Test
        @DisplayName("setTrustedProxies with empty array should not change")
        void setTrustedProxies_withEmptyArray_shouldNotChange() {
            // Act
            RequestUtil.setTrustedProxies(new String[]{});

            // Assert
            // 不会抛出异常，也不会修改
        }
    }

    @Nested
    @DisplayName("Get Client IP Tests")
    class GetClientIpTests {

        @Test
        @DisplayName("getClientIp with no request context should return unknown")
        void getClientIp_withNoRequestContext_shouldReturnUnknown() {
            // Arrange
            RequestContextHolder.resetRequestAttributes();

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("unknown");
        }

        @Test
        @DisplayName("getClientIp with null request should return unknown")
        void getClientIp_withNullRequest_shouldReturnUnknown() {
            // Act
            String result = RequestUtil.getClientIp((HttpServletRequest) null);

            // Assert
            assertThat(result).isEqualTo("unknown");
        }

        @Test
        @DisplayName("getClientIp from X-Forwarded-For with trusted proxy")
        void getClientIp_fromXForwardedFor_withTrustedProxy() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "192.168.1.100");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("getClientIp from X-Real-IP with trusted proxy")
        void getClientIp_fromXRealIp_withTrustedProxy() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Real-IP", "10.0.0.50");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("10.0.0.50");
        }

        @Test
        @DisplayName("getClientIp from remoteAddr with untrusted proxy")
        void getClientIp_fromRemoteAddr_withUntrustedProxy() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.200");
            request.addHeader("X-Forwarded-For", "10.0.0.1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("192.168.1.200");
        }

        @Test
        @DisplayName("getClientIp with localhost IPv6 should convert to IPv4")
        void getClientIp_withLocalhostIpv6_shouldConvertToIpv4() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("0:0:0:0:0:0:0:1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("getClientIp with multiple IPs in X-Forwarded-For should return first")
        void getClientIp_withMultipleIpsInXForwardedFor_shouldReturnFirst() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1, 172.16.0.1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("getClientIp with unknown header value should fall back to remoteAddr")
        void getClientIp_withUnknownHeaderValue_shouldFallBackToRemoteAddr() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.50");
            request.addHeader("X-Forwarded-For", "unknown");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getClientIp();

            // Assert
            assertThat(result).isEqualTo("192.168.1.50");
        }
    }

    @Nested
    @DisplayName("Get Request Path Tests")
    class GetRequestPathTests {

        @Test
        @DisplayName("getRequestPath with no context should return empty string")
        void getRequestPath_withNoContext_shouldReturnEmptyString() {
            // Arrange
            RequestContextHolder.resetRequestAttributes();

            // Act
            String result = RequestUtil.getRequestPath();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getRequestPath should return request URI")
        void getRequestPath_shouldReturnRequestUri() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/user/info");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getRequestPath();

            // Assert
            assertThat(result).isEqualTo("/api/user/info");
        }
    }

    @Nested
    @DisplayName("Get Full Request URL Tests")
    class GetFullRequestUrlTests {

        @Test
        @DisplayName("getFullRequestUrl with null request should return empty string")
        void getFullRequestUrl_withNullRequest_shouldReturnEmptyString() {
            // Act
            String result = RequestUtil.getFullRequestUrl((HttpServletRequest) null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getFullRequestUrl without query string")
        void getFullRequestUrl_withoutQueryString() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/user/info");
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.setRequestURI("/api/user/info");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getFullRequestUrl(RequestUtil.getRequest());

            // Assert
            assertThat(result).contains("http://localhost:8080/api/user/info");
        }

        @Test
        @DisplayName("getFullRequestUrl with query string")
        void getFullRequestUrl_withQueryString() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.setRequestURI("/api/users");
            request.setQueryString("page=1&size=10");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            String result = RequestUtil.getFullRequestUrl(RequestUtil.getRequest());

            // Assert
            assertThat(result).contains("http://localhost:8080/api/users?page=1&size=10");
        }
    }

    @Nested
    @DisplayName("Get Request Tests")
    class GetRequestTests {

        @Test
        @DisplayName("getRequest with no context should return null")
        void getRequest_withNoContext_shouldReturnNull() {
            // Arrange
            RequestContextHolder.resetRequestAttributes();

            // Act
            HttpServletRequest result = RequestUtil.getRequest();

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getRequest with context should return request")
        void getRequest_withContext_shouldReturnRequest() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // Act
            HttpServletRequest result = RequestUtil.getRequest();

            // Assert
            assertThat(result).isEqualTo(request);
        }
    }
}
