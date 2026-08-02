package com.cartethyia.easyorange.message.application.service;

import com.cartethyia.easyorange.framework.cache.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息发送频率限制 — 应用层服务，非领域逻辑。
 * <p>
 * 限流是运维/操作性关注点：阈值(5条/秒)、Redis 原子操作、本地降级计数器
 * 均不属于"消息"业务领域，故放在 application 层而非 domain 层。
 */
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<Object, Object> redisTemplate;

    private static final String MESSAGE_RATE_KEY = "chat:rate:message:%s";
    private static final String TYPING_RATE_KEY = "chat:rate:typing:%s";

    private static final int MAX_MESSAGES_PER_SECOND = 5;
    private static final int MAX_TYPING_PER_2SECONDS = 1;
    private static final Duration RATE_WINDOW = Duration.ofSeconds(1);

    private final AtomicInteger localCounter = new AtomicInteger(0);

    public RateLimiterService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks whether the user is allowed to send a message, respecting the
     * per-user message rate limit. Falls back to a local counter if Redis is
     * unavailable.
     */
    public boolean allowSendMessage(String userId) {
        String key = MESSAGE_RATE_KEY.formatted(userId);

        try {
            Integer count = CacheUtils.cast(redisTemplate.opsForValue().get(key), Integer.class);
            if (count == null) {
                redisTemplate.opsForValue().set(key, 1, RATE_WINDOW.getSeconds(), TimeUnit.SECONDS);
                return true;
            }

            if (count >= MAX_MESSAGES_PER_SECOND) {
                log.debug("action=rate_limited userId={} type=message count={}", userId, count);
                return false;
            }

            redisTemplate.opsForValue().increment(key, 1L);
            return true;
        } catch (Exception e) {
            log.warn("action=rate_limit_fallback userId={}", userId, e);
            int current = localCounter.getAndIncrement();
            if (current >= MAX_MESSAGES_PER_SECOND) {
                localCounter.set(0);
                return false;
            }
            return true;
        }
    }

    /**
     * Checks whether the user is allowed to send a typing indicator,
     * limited to one per 2 seconds per user.
     */
    public boolean allowTyping(String userId) {
        String key = TYPING_RATE_KEY.formatted(userId);

        try {
            Integer count = CacheUtils.cast(redisTemplate.opsForValue().get(key), Integer.class);
            if (count == null) {
                redisTemplate.opsForValue().set(key, 1, 2, TimeUnit.SECONDS);
                return true;
            }

            if (count >= MAX_TYPING_PER_2SECONDS) {
                return false;
            }

            redisTemplate.opsForValue().increment(key, 1L);
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
