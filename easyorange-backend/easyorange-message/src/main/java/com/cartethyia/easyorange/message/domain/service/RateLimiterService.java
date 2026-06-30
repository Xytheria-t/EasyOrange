package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RedisCache redisCache;

    private static final String MESSAGE_RATE_KEY = "chat:rate:message:%s";
    private static final String TYPING_RATE_KEY = "chat:rate:typing:%s";

    private static final int MAX_MESSAGES_PER_SECOND = 5;
    private static final int MAX_TYPING_PER_2SECONDS = 1;
    private static final Duration RATE_WINDOW = Duration.ofSeconds(1);

    private final AtomicInteger localCounter = new AtomicInteger(0);

    /**
     * Constructs a rate limiter service backed by Redis with a local fallback.
     *
     * @param redisCache Redis cache for distributed rate limiting
     */
    public RateLimiterService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * Checks whether the user is allowed to send a message, respecting the
     * per-user message rate limit. Falls back to a local counter if Redis is
     * unavailable.
     *
     * @param userId the ID of the user attempting to send a message
     * @return true if the message is allowed, false if the rate limit is exceeded
     */
    public boolean allowSendMessage(String userId) {
        String key = MESSAGE_RATE_KEY.formatted(userId);

        try {
            Integer count = redisCache.get(key, Integer.class);
            if (count == null) {
                redisCache.set(key, 1, RATE_WINDOW.getSeconds(), TimeUnit.SECONDS);
                return true;
            }

            if (count >= MAX_MESSAGES_PER_SECOND) {
                log.debug("action=rate_limited userId={} type=message count={}", userId, count);
                return false;
            }

            redisCache.increment(key, 1L);
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
     *
     * @param userId the ID of the user sending the typing indicator
     * @return true if the typing indicator is allowed, false if throttled
     */
    public boolean allowTyping(String userId) {
        String key = TYPING_RATE_KEY.formatted(userId);

        try {
            Integer count = redisCache.get(key, Integer.class);
            if (count == null) {
                redisCache.set(key, 1, 2, TimeUnit.SECONDS);
                return true;
            }

            if (count >= MAX_TYPING_PER_2SECONDS) {
                return false;
            }

            redisCache.increment(key, 1L);
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
