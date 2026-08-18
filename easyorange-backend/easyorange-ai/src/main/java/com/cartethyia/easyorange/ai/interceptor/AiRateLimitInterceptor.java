package com.cartethyia.easyorange.ai.interceptor;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * AI 模块限流拦截器 — scope 粒度令牌桶（如对话 20 次/分/用户）。
 * <p>
 * 降级分工：限流超限返回 429（fail-open 容忍 Redis 故障）；
 * LLM 供应商故障的旧回答兜底在 {@code AiChatService} 服务层（stale-while-error），
 * 拦截器不再承担缓存职责（历史设计因请求体字节依赖不可靠而移除）。
 */
@Slf4j
@Component
@NullMarked
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private final DistributedRateLimiter distributedRateLimiter;
    private final AiProperties aiProperties;
    private final ErrorResponseWriter errorResponseWriter;

    public AiRateLimitInterceptor(
            DistributedRateLimiter distributedRateLimiter,
            AiProperties aiProperties,
            ErrorResponseWriter errorResponseWriter) {
        this.distributedRateLimiter = distributedRateLimiter;
        this.aiProperties = aiProperties;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/ai/")) {
            return true;
        }

        AiCallScope scope = AiCallScope.fromUri(uri);
        String userKey = resolveUserKey(request);
        String bucketKey = scope.rateLimitKeyPrefix() + userKey;

        try {
            // Redisson RRateLimiter 令牌桶 — 原子化取桶/补桶/扣桶，解决 increment+expire 的原子性缺口
            boolean allowed = distributedRateLimiter.tryAcquire(bucketKey, scope.getRatePerMinute(), 60);

            if (!allowed) {
                log.debug("AI rate limit exceeded: scope={}, user={}", scope, userKey);
                // 与全局约定一致，限流错误也返回 Result 信封（code=A0429）
                errorResponseWriter.write(response, 429, ResultCode.TOO_MANY_REQUESTS, "AI 服务繁忙，请稍后重试");
                return false;
            }
        } catch (Exception e) {
            if (aiProperties.getRateLimit().isFailOpen()) {
                log.warn("Redis unavailable, fail-open for AI rate limit", e);
                return true;
            }
            throw e;
        }

        return true;
    }

    private String resolveUserKey(HttpServletRequest request) {
        return SecurityContextUtil.getCurrentUserId()
                .map(id -> "user:" + id)
                .orElseGet(() -> "ip:" + RequestUtil.getClientIp(request));
    }
}
