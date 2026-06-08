package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private final RedisCache redisCache;

    @Override
    public Long countAttempts(String identifier) {
        return redisCache.get(LoginCacheConstants.buildAttemptsKey(identifier), Long.class);
    }

    @Override
    public long incrementAttempts(String identifier, long expireMinutes) {
        String key = LoginCacheConstants.buildAttemptsKey(identifier);
        Long count = redisCache.increment(key);
        // Only set TTL on first increment — implements fixed window, not sliding
        if (count != null && count == 1) {
            redisCache.expire(key, expireMinutes, TimeUnit.MINUTES);
        }
        return count != null ? count : 0;
    }

    @Override
    public void clearAttempts(String identifier) {
        redisCache.delete(LoginCacheConstants.buildAttemptsKey(identifier));
    }

    @Override
    public long getRemainingLockSeconds(String identifier) {
        String key = LoginCacheConstants.buildAttemptsKey(identifier);
        return Math.max(0, redisCache.getExpire(key, TimeUnit.SECONDS));
    }
}
