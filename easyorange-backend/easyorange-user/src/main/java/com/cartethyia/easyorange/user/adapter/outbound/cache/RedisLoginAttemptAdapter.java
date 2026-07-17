package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public long incrementAndGet(String identifier, Duration expireAfter) {
        String key = LoginCacheConstants.buildAttemptsKey(identifier);
        Long count = redisTemplate.opsForValue().increment(key);
        // Only set TTL on first increment — implements fixed window, not sliding
        if (count != null && count == 1) {
            redisTemplate.expire(key, expireAfter.toMinutes(), TimeUnit.MINUTES);
        }
        return count != null ? count : 0;
    }

    @Override
    public void clear(String identifier) {
        redisTemplate.delete(LoginCacheConstants.buildAttemptsKey(identifier));
    }

    @Override
    public long getRemainingLockSeconds(String identifier) {
        String key = LoginCacheConstants.buildAttemptsKey(identifier);
        return Math.max(0, redisTemplate.getExpire(key, TimeUnit.SECONDS));
    }
}
