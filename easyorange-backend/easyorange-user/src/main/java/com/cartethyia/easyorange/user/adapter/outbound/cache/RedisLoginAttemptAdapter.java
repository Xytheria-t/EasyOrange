package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private final RedisCache redisCache;

    @Override
    public Long getAttempts(String account) {
        return redisCache.get(LoginCacheConstants.buildAttemptsKey(account), Long.class);
    }

    @Override
    public long incrementAttempts(String account) {
        Long count = redisCache.increment(LoginCacheConstants.buildAttemptsKey(account));
        return count != null ? count : 0;
    }

    @Override
    public void expireAttempts(String account, long minutes) {
        redisCache.expire(LoginCacheConstants.buildAttemptsKey(account), minutes, TimeUnit.MINUTES);
    }

    @Override
    public void clearAttempts(String account) {
        redisCache.delete(LoginCacheConstants.buildAttemptsKey(account));
    }
}
