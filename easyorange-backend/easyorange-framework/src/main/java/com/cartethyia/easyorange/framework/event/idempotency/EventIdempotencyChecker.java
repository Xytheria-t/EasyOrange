package com.cartethyia.easyorange.framework.event.idempotency;

import com.cartethyia.easyorange.framework.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventIdempotencyChecker {

    private final RedisCache redisCache;

    private static final String EVENT_KEY_PREFIX = "event:idempotency:";
    private static final long DEFAULT_EXPIRE_HOURS = 24;

    public boolean isDuplicate(String eventType, String eventId) {
        if (eventType == null || eventId == null) {
            return false;
        }
        String key = EVENT_KEY_PREFIX + eventType + ":" + eventId;
        return Boolean.TRUE.equals(redisCache.hasKey(key));
    }

    public boolean tryMark(String eventType, String eventId) {
        if (eventType == null || eventId == null) {
            return true;
        }
        String key = EVENT_KEY_PREFIX + eventType + ":" + eventId;
        Boolean success = redisCache.setIfAbsent(key, "1", DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(success)) {
            return true;
        }
        log.warn("action=duplicate_event_detected eventType={} eventId={}", eventType, eventId);
        return false;
    }

    public void markProcessed(String eventType, String eventId) {
        if (eventType == null || eventId == null) {
            return;
        }
        String key = EVENT_KEY_PREFIX + eventType + ":" + eventId;
        redisCache.set(key, "1", DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    public void remove(String eventType, String eventId) {
        if (eventType == null || eventId == null) {
            return;
        }
        String key = EVENT_KEY_PREFIX + eventType + ":" + eventId;
        redisCache.delete(key);
    }
}
