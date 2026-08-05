package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import com.cartethyia.easyorange.framework.web.idempotency.CachedResponse;
import com.cartethyia.easyorange.framework.web.idempotency.IdempotencyService;
import com.cartethyia.easyorange.framework.web.idempotency.IdempotentOperation;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Idempotency-Key 幂等过滤器 — 单元测试。
 * <p>
 * 核心职责：命中路径 + 写方法 + 携带 key 才交给 {@link IdempotencyService}；
 * 成功响应经响应包装（ContentCachingResponseWrapper）抓取后回放；非 2xx 不缓存直接提交。
 * </p>
 * <p>
 * 并发抢锁/缓存语义由 {@code RedisIdempotencyServiceTest} 覆盖，这里只测过滤器行为。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Idempotency-Key 幂等过滤器")
class IdempotencyKeyFilterTest {

    private static final String KEY = "order-create-1";
    private static final String BODY = "{\"ok\":true}";

    @Mock
    private IdempotencyService idempotencyService;

    private IdempotencyProperties properties;
    private IdempotencyKeyFilter filter;

    @BeforeEach
    void setUp() {
        properties = new IdempotencyProperties();
        properties.setEnabled(true);
        properties.setPathPatterns(List.of("/api/orders"));
        properties.setMethods(Set.of("POST", "PUT", "PATCH"));
        filter = new IdempotencyKeyFilter(idempotencyService, properties);
    }

    private MockHttpServletRequest keyedWriteRequest() {
        var req = new MockHttpServletRequest("POST", "/api/orders");
        req.addHeader("Idempotency-Key", KEY);
        return req;
    }

    // ==================== 透传（不调用幂等服务） ====================

    @Test
    @DisplayName("禁用时透传，不调用幂等服务")
    void disabled_passesThrough() throws Exception {
        properties.setEnabled(false);
        var invoked = new AtomicBoolean(false);

        filter.doFilter(keyedWriteRequest(), new MockHttpServletResponse(),
                (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        verify(idempotencyService, never()).execute(any(), anyLong(), any());
    }

    @Test
    @DisplayName("未携带 Idempotency-Key 头时透传")
    void missingHeader_passesThrough() throws Exception {
        var invoked = new AtomicBoolean(false);
        var req = new MockHttpServletRequest("POST", "/api/orders");

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        verify(idempotencyService, never()).execute(any(), anyLong(), any());
    }

    @Test
    @DisplayName("非写方法（GET）透传")
    void readMethod_passesThrough() throws Exception {
        var invoked = new AtomicBoolean(false);
        var req = new MockHttpServletRequest("GET", "/api/orders");
        req.addHeader("Idempotency-Key", KEY);

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        verify(idempotencyService, never()).execute(any(), anyLong(), any());
    }

    @Test
    @DisplayName("路径未命中配置模式时透传")
    void unmatchedPath_passesThrough() throws Exception {
        var invoked = new AtomicBoolean(false);
        var req = new MockHttpServletRequest("POST", "/api/products");
        req.addHeader("Idempotency-Key", KEY);

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        verify(idempotencyService, never()).execute(any(), anyLong(), any());
    }

    // ==================== 回放 / 抓取 ====================

    @Test
    @DisplayName("缓存命中：回放缓存响应，不执行后续链路")
    void cacheHit_replaysResponse() throws Exception {
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);
        when(idempotencyService.execute(anyString(), anyLong(), any()))
                .thenReturn(new CachedResponse(200, "application/json",
                        BODY.getBytes(StandardCharsets.UTF_8)));

        filter.doFilter(keyedWriteRequest(), res, (r, s) -> invoked.set(true));

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).isEqualTo(BODY);
        assertThat(invoked).isFalse();
    }

    @Test
    @DisplayName("首次请求：执行链路并抓取响应，回放给客户端")
    void freshRequest_runsChainAndReplays() throws Exception {
        var res = new MockHttpServletResponse();
        when(idempotencyService.execute(anyString(), anyLong(), any())).thenAnswer(inv -> {
            IdempotentOperation<CachedResponse> op = inv.getArgument(2);
            return op.execute();
        });
        FilterChain chain = (r, s) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) s;
            http.setStatus(200);
            http.setContentType("application/json");
            http.getWriter().write(BODY);
        };

        filter.doFilter(keyedWriteRequest(), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("非 2xx 响应不缓存并提交给客户端，不抛异常")
    void errorResponse_notCachedAndCommitted() throws Exception {
        var res = new MockHttpServletResponse();
        when(idempotencyService.execute(anyString(), anyLong(), any())).thenAnswer(inv -> {
            IdempotentOperation<CachedResponse> op = inv.getArgument(2);
            return op.execute(); // 内部提交并抛 NonCacheableResponseException
        });
        FilterChain chain = (r, s) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) s;
            http.setStatus(400);
            http.setContentType("application/json");
            http.getWriter().write("{\"error\":\"bad\"}");
        };

        filter.doFilter(keyedWriteRequest(), res, chain);

        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(res.getContentAsString()).isEqualTo("{\"error\":\"bad\"}");
    }
}