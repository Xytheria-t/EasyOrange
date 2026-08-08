package com.cartethyia.easyorange.framework.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.RepeatSubmitConfig;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Rule;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.LocalRateLimiter;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private RateLimitFilterProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitFilterProperties();
        filter = new RateLimitFilter(
                properties,
                redisTemplate,
                localRateLimiter,
                distributedRateLimiter,
                new ErrorResponseWriter(new ObjectMapper()),
                handlerMappingsProvider);
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
        Rule rule = new Rule();
        rule.setPathPattern(pathPattern);
        rule.setStrategy("local");
        rule.setMaxRequests(5);
        rule.setWindowSeconds(60);
        return rule;
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
        properties.setRules(List.of(localRule("/api/ai/**")));
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
        properties.setRules(List.of(localRule("/api/products")));
        stubHandler(handlerFor("noSkip"));
        when(localRateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);

        var req = new MockHttpServletRequest("GET", "/api/products");
        var res = new MockHttpServletResponse();
        var invoked = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, s) -> invoked.set(true));

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(invoked).isFalse();
        verify(localRateLimiter).tryAcquire(anyString(), anyInt(), anyLong());
    }

    // ==================== 防重：@SkipRepeatSubmit 豁免 ====================

    @Test
    @DisplayName("写方法带 @SkipRepeatSubmit：跳过防重，链继续")
    void writeMethod_withSkipRepeatSubmit_passesThrough() throws Exception {
        RepeatSubmitConfig rs = properties.getRepeatSubmit();
        rs.setEnabled(true);
        rs.setIntervalMs(3000);
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
        RepeatSubmitConfig rs = properties.getRepeatSubmit();
        rs.setEnabled(true);
        rs.setIntervalMs(3000);
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
        assertThat(invoked).isFalse();
        verify(valueOps).setIfAbsent(anyString(), any(), anyLong(), any());
    }
}
