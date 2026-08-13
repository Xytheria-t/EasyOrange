package com.cartethyia.easyorange.ai.interceptor;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@NullMarked
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private final DistributedRateLimiter distributedRateLimiter;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Cache<String, Object> staleCache;

    public AiRateLimitInterceptor(
            DistributedRateLimiter distributedRateLimiter,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache) {
        this.distributedRateLimiter = distributedRateLimiter;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.staleCache = staleCache;
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

                String staleKey = extractStaleKey(request, scope);
                if (staleKey != null) {
                    Object stale = staleCache.getIfPresent(staleKey);
                    if (stale != null) {
                        log.info("Serving stale cache for rate-limited request: {}", staleKey);
                        writeJson(response, 200, stale);
                        return false;
                    }
                }

                // 与全局约定一致，限流错误也返回 Result 信封（code=A0429）
                writeJson(response, 429, Result.error(ResultCode.TOO_MANY_REQUESTS, "AI 服务繁忙，请稍后重试"));
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

    private @Nullable String extractStaleKey(HttpServletRequest request, AiCallScope scope) {
        try {
            String body = request.getReader().lines().reduce("", (a, b) -> a + b);
            if (body.isEmpty()) return null;
            String fp = fingerprint(body);
            return scope.cacheKeyPrefix() + fp;
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable String fingerprint(String content) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
