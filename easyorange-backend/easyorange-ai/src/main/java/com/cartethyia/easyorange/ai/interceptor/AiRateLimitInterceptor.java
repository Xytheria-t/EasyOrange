package com.cartethyia.easyorange.ai.interceptor;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private final RedisCache redisCache;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Cache<String, Object> staleCache;

    public AiRateLimitInterceptor(
            RedisCache redisCache,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache) {
        this.redisCache = redisCache;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.staleCache = staleCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/ai/")) {
            return true;
        }

        AiCallScope scope = AiCallScope.fromUri(uri);
        String userKey = resolveUserKey(request);
        String bucketKey = scope.rateLimitKeyPrefix() + userKey;

        try {
            Long count = redisCache.increment(bucketKey);
            if (count != null && count == 1) {
                redisCache.expire(bucketKey, 60, TimeUnit.SECONDS);
            }

            if (count != null && count > scope.getRatePerMinute()) {
                log.debug("AI rate limit exceeded: scope={}, user={}, count={}",
                        scope, userKey, count);

                String staleKey = extractStaleKey(request, scope);
                if (staleKey != null) {
                    Object stale = staleCache.getIfPresent(staleKey);
                    if (stale != null) {
                        log.info("Serving stale cache for rate-limited request: {}", staleKey);
                        writeJson(response, 200, stale);
                        return false;
                    }
                }

                writeJson(response, 429,
                        Map.of("success", false, "message", "AI 服务繁忙，请稍后重试"));
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
                .orElseGet(() -> "ip:" + getClientIp(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private String extractStaleKey(HttpServletRequest request, AiCallScope scope) {
        try {
            String body = request.getReader().lines()
                    .reduce("", (a, b) -> a + b);
            if (body.isEmpty()) return null;
            String fp = fingerprint(body);
            return scope.cacheKeyPrefix() + fp;
        } catch (Exception e) {
            return null;
        }
    }

    private static String fingerprint(String content) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private void writeJson(HttpServletResponse response, int status, Object body)
            throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
