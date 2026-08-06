package com.cartethyia.easyorange.message.application.service;

import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息发送频率限制 — 应用层服务，非领域逻辑。
 * <p>
 * 限流是运维/操作性关注点：基于 Redisson RRateLimiter 令牌桶（{@link DistributedRateLimiter}），
 * 解决手写 {@code get}+{@code increment} 的原子性缺口；Redis 不可用时 fail-open 放行，
 * 与框架 {@code RateLimitFilter} / {@code AiRateLimitInterceptor} 的降级策略一致。
 */
@Slf4j
public class RateLimiterService {

    private final DistributedRateLimiter distributedRateLimiter;

    private static final String MESSAGE_RATE_KEY = "eo:rate:message:%s";

    private static final int MAX_MESSAGES_PER_SECOND = 5;

    public RateLimiterService(DistributedRateLimiter distributedRateLimiter) {
        this.distributedRateLimiter = distributedRateLimiter;
    }

    /**
     * Checks whether the user is allowed to send a message, respecting the
     * per-user message rate limit. Fails open (allows) if Redis is unavailable.
     */
    public boolean allowSendMessage(String userId) {
        try {
            return distributedRateLimiter.tryAcquire(MESSAGE_RATE_KEY.formatted(userId), MAX_MESSAGES_PER_SECOND, 1);
        } catch (Exception e) {
            log.warn("action=rate_limit_fallback userId={}", userId, e);
            return true;
        }
    }
}
