package com.cartethyia.easyorange.framework.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Rule;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Strategy;
import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.LocalRateLimiter;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

/**
 * RateLimitFilter 限流 + 防重跳过注解 — 单元测试。
 * <p>
 * 覆盖核心职责：命中规则/写请求时才解析 handler（懒解析）、
 * {@code @SkipRateLimit} / {@code @SkipRepeatSubmit} 标记注解正确豁免对应检查。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter 限流 + 防重跳过注解")
class RateLimitFilterTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private LocalRateLimiter localRateLimiter;

    @Mock
    private DistributedRateLimiter distributedRateLimiter;

    @Mock
    @SuppressWarnings("rawtypes")
    private ObjectProvider<List<HandlerMapping>> handlerMappingsProvider;

    @Mock
    private HandlerMapping handlerMapping;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = newFilter(PropertyBindings.bind(RateLimitFilterProperties.class));
    }

    private RateLimitFilter newFilter(RateLimitFilterProperties properties) {
        return new RateLimitFilter(
                properties,
                redisTemplate,
                localRateLimiter,
                distributedRateLimiter,
                new ErrorResponseWriter(new ObjectMapper()),
                handlerMappingsProvider);
    }

    private RateLimitFilter filterWithRules(Rule... rules) {
        return newFilter(new RateLimitFilterProperties(true, List.of(rules), null));
    }

    // ==================== 测试用 Controller 与 handler 解析 ====================

    /** 测试用 Controller：方法级标记 Skip 注解，验证豁免逻辑。 */
    static class TestController {

        @SkipRateLimit
        public void skipRateLimit() {}

        @SkipRepeatSubmit
        public void skipRepeatSubmit() {}

        public void noSkip() {}
    }

    private HandlerMethod handlerFor(String methodName) throws Exception {
        return new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod(methodName));
    }

    private void stubHandler(HandlerMethod handler) throws Exception {
        when(handlerMappingsProvider.getIfAvailable(any(Supplier.class))).thenReturn(List.of(handlerMapping));
        when(handlerMapping.getHandler(any())).thenReturn(new HandlerExecutionChain(handler));
    }

    private Rule localRule(String pathPattern) {
        return new Rule(pathPattern, null, Strategy.LOCAL, 5, 60, "请求过于频繁，请稍后重试");
    }

    // ==================== 懒解析：GET 未命中规则不解析 handler ====================

    @Test
    @DisplayName("GET 且未命中限流规则：不解析 handler（懒解析优化）")
    void read_noMatchingRule_doesNotResolveHandler() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/other");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        verify(handlerMappingsProvider, never()).getIfAvailable(any(Supplier.class));
        verify(handlerMapping, never()).getHandler(any());
    }

    // ==================== 限流：@SkipRateLimit 豁免 ====================

    @Test
    @DisplayName("命中限流规则但方法带 @SkipRateLimit：放行，不触发限流")
    void matchedRule_withSkipRateLimit_passesThrough() throws Exception {
        filter = filterWithRules(localRule("/api/ai/**"));
        stubHandler(handlerFor("skipRateLimit"));

        var req = new MockHttpServletRequest("GET", "/api/ai/chat");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(res.getStatus()).isEqualTo(200);
        verify(localRateLimiter, never()).tryAcquire(anyString(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("命中限流规则且无跳过标注：本地限流拒绝返回 429")
    void matchedRule_withoutSkip_localRateLimitDenies() throws Exception {
        filter = filterWithRules(localRule("/api/products"));
        stubHandler(handlerFor("noSkip"));
        when(localRateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);

        var req = new MockHttpServletRequest("GET", "/api/products");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("A0429");
        assertThat(invoked).isFalse();
        verify(localRateLimiter).tryAcquire(anyString(), anyInt(), anyLong());
    }

    // ==================== 防重：@SkipRepeatSubmit 豁免 ====================

    @Test
    @DisplayName("写方法带 @SkipRepeatSubmit：跳过防重，链继续")
    void writeMethod_withSkipRepeatSubmit_passesThrough() throws Exception {
        stubHandler(handlerFor("skipRepeatSubmit"));

        var req = new MockHttpServletRequest("POST", "/api/external/callback");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(res.getStatus()).isEqualTo(200);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("写方法无跳过标注：防重命中返回 429")
    void writeMethod_withoutSkip_repeatSubmitDenies() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<Object, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(false);
        stubHandler(handlerFor("noSkip"));

        var req = new MockHttpServletRequest("POST", "/api/orders");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("A0429");
        assertThat(invoked).isFalse();
        verify(valueOps).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("防重 key 区分方法：同一 URI 的收藏与取消收藏不互相拦截")
    void repeatSubmit_keyDistinguishesMethod() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<Object, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        stubHandler(handlerFor("noSkip"));

        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/favorites/2001"), new MockHttpServletResponse(), (r, s) -> {});
        filter.doFilter(
                new MockHttpServletRequest("DELETE", "/api/favorites/2001"),
                new MockHttpServletResponse(),
                (r, s) -> {});

        var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps, times(2)).setIfAbsent(keyCaptor.capture(), any(), anyLong(), any());

        assertThat(keyCaptor.getAllValues()).doesNotHaveDuplicates();
        assertThat(keyCaptor.getAllValues().get(0)).contains(":POST:/api/favorites/2001:");
        assertThat(keyCaptor.getAllValues().get(1)).contains(":DELETE:/api/favorites/2001:");
    }

    // ==================== multipart：不预读 body ====================

    @Test
    @DisplayName("multipart 写请求：原样透传不包装（包装会读空输入流，容器随后解析 parts 必失败）")
    void multipartWriteRequest_passesThroughUnwrapped() throws Exception {
        stubHandler(handlerFor("noSkip"));

        var req = new MockHttpServletRequest("POST", "/api/file/upload");
        req.setContentType("multipart/form-data; boundary=----eoBoundary");
        req.setContent("------eoBoundary--".getBytes(StandardCharsets.UTF_8));
        var res = new MockHttpServletResponse();
        var downstream = new AtomicReference<Object>();

        filter.doFilter(req, res, (r, s) -> downstream.set(r));

        // 同一实例 = 未被 CachedBodyHttpServletRequestWrapper 包裹，原始流仍留给容器解析 parts
        assertThat(downstream.get()).isSameAs(req);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("multipart 写请求：无 body 可算防重 key，跳过防重（不误拦截上传）")
    void multipartWriteRequest_skipsRepeatSubmit() throws Exception {
        stubHandler(handlerFor("noSkip"));

        var req = new MockHttpServletRequest("POST", "/api/file/upload");
        req.setContentType("multipart/form-data; boundary=----eoBoundary");
        req.setContent("------eoBoundary--".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});

        verify(redisTemplate, never()).opsForValue();
    }
}
