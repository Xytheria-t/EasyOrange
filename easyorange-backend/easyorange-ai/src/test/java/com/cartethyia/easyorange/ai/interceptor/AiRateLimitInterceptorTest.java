package com.cartethyia.easyorange.ai.interceptor;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiRateLimitInterceptor 测试")
class AiRateLimitInterceptorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private AiProperties.RateLimit rateLimitProps;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AiMetricsService aiMetricsService;

    private Cache<String, Object> staleCache;
    private ObjectMapper objectMapper;
    private AiRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(aiProperties.getRateLimit()).thenReturn(rateLimitProps);
        lenient().when(rateLimitProps.isFailOpen()).thenReturn(true);
        lenient().doNothing().when(aiMetricsService).recordRateLimitRejected(anyString());
        lenient().doNothing().when(aiMetricsService).recordRateLimitStaleServed(anyString());
        lenient().doNothing().when(aiMetricsService).recordRateLimitFailOpen(anyString());

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        staleCache = Caffeine.newBuilder().build();
        objectMapper = new ObjectMapper();
        interceptor = new AiRateLimitInterceptor(redisTemplate, aiProperties, objectMapper, staleCache, aiMetricsService);
    }

    @Test
    @DisplayName("非 AI 路径放行")
    void nonAiPathPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("限流未超时放行")
    void withinLimitPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/review");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 异常时 fail-open")
    void redisExceptionFailOpen() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/review");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verify(aiMetricsService).recordRateLimitFailOpen("REVIEW");
    }

    @Test
    @DisplayName("限流超时返回 429")
    void rateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/review");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(valueOps.increment(anyString())).thenReturn(11L);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        when(response.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(aiMetricsService).recordRateLimitRejected("REVIEW");
    }

    @Test
    @DisplayName("X-Forwarded-For 头解析")
    void xForwardedForHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/review");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }
}
