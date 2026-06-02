package com.cartethyia.easyorange.framework.event.idempotency;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EventIdempotencyChecker {

    private static final String EVENT_LOCK_PREFIX = "eo:event:lock:";
    private static final String EVENT_DONE_PREFIX = "eo:event:done:";
    private static final long LOCK_TIMEOUT_SECONDS = 30;
    private static final long DONE_TTL_HOURS = 24;

    private final RedisCache redisCache;

    public EventIdempotencyChecker(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public boolean isDuplicate(String eventType, String eventId) {
        return redisCache.hasKey(EVENT_DONE_PREFIX + eventType + ":" + eventId);
    }

    public boolean tryMark(String eventType, String eventId) {
        String lockKey = EVENT_LOCK_PREFIX + eventType + ":" + eventId;
        try {
            boolean locked = redisCache.tryLock(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }
            try {
                if (isDuplicate(eventType, eventId)) {
                    return false;
                }
                redisCache.set(EVENT_DONE_PREFIX + eventType + ":" + eventId, "1", DONE_TTL_HOURS, TimeUnit.HOURS);
                return true;
            } finally {
                redisCache.unlock(lockKey, "1");
            }
        } catch (Exception e) {
            log.error("Event idempotency check failed: eventType={}, eventId={}", eventType, eventId, e);
            return true;
        }
    }
}