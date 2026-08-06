package com.cartethyia.easyorange.framework.event.idempotency;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventIdempotencyChecker {

    private static final String EVENT_LOCK_PREFIX = "eo:event:lock:";
    private static final String EVENT_DONE_PREFIX = "eo:event:done:";
    private static final long LOCK_TIMEOUT_SECONDS = 30;
    private static final long DONE_TTL_HOURS = 24;

    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public EventIdempotencyChecker(RedisTemplate<Object, Object> redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    public boolean isDuplicate(String eventType, String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(EVENT_DONE_PREFIX + eventType + ":" + eventId));
    }

    public boolean tryMark(String eventType, String eventId) {
        String lockKey = EVENT_LOCK_PREFIX + eventType + ":" + eventId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(0, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }
            try {
                if (isDuplicate(eventType, eventId)) {
                    return false;
                }
                redisTemplate
                        .opsForValue()
                        .set(EVENT_DONE_PREFIX + eventType + ":" + eventId, "1", DONE_TTL_HOURS, TimeUnit.HOURS);
                return true;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("Event idempotency check failed: eventType={}, eventId={}", eventType, eventId, e);
            return true;
        }
    }
}
