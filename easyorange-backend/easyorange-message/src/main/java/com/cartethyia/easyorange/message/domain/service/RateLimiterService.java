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

    public RateLimiterService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public boolean allowSendMessage(Long userId) {
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
            log.warn("action=rate_limit_fallback userId={} error={}", userId, e.getMessage());
            int current = localCounter.getAndIncrement();
            if (current >= MAX_MESSAGES_PER_SECOND) {
                localCounter.set(0);
                return false;
            }
            return true;
        }
    }

    public boolean allowTyping(Long userId) {
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
