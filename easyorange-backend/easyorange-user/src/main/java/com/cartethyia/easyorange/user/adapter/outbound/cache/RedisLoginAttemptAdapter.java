package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
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
    public long incrementAndExpire(String account, long expireMinutes) {
        String key = LoginCacheConstants.buildAttemptsKey(account);
        Long count = redisCache.increment(key);
        redisCache.expire(key, expireMinutes, TimeUnit.MINUTES);
        return count != null ? count : 0;
    }

    @Override
    public void clearAttempts(String account) {
        redisCache.delete(LoginCacheConstants.buildAttemptsKey(account));
    }
}
