package com.cartethyia.easyorange.ai.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor 测试")
class RateLimitInterceptorTest {

    @Mock
    private DistributedRateLimiter distributedRateLimiter;

    @Mock
    private HttpServletRequest request;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(
                distributedRateLimiter,
                PropertyBindings.bind(AiProperties.class),
                new ErrorResponseWriter(new ObjectMapper()));
    }

    @Test
    @DisplayName("非 AI 路径放行")
    void nonAiPathPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("限流未超时放行")
    void withinLimitPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/auto-listing");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                .thenReturn(true);

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 异常时 fail-open")
    void redisExceptionFailOpen() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/auto-listing");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Redis down"));

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("限流超时返回 429 且响应体为 Result 信封")
    void rateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/auto-listing");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                .thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("A0429");
    }

    @Test
    @DisplayName("X-Forwarded-For 头解析")
    void xForwardedForHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/auto-listing");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(distributedRateLimiter.tryAcquire(anyString(), anyLong(), anyLong()))
                .thenReturn(true);

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
        // 桶 key 必须落在 XFF 首段（真实客户端）——代理跳数或常量进 key 等于所有人共享一个桶
        var bucketKey = ArgumentCaptor.forClass(String.class);
        verify(distributedRateLimiter).tryAcquire(bucketKey.capture(), anyLong(), anyLong());
        assertThat(bucketKey.getValue()).endsWith("ip:192.168.1.1").doesNotContain("10.0.0.1");
    }
}
