package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private final RedisTemplate<Object, Object> redisTemplate;

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
    public long getAttempts(String identifier) {
        String key = LoginCacheConstants.buildAttemptsKey(identifier);
        // 计数键值由 INCR 写入，非 JSON 格式，故用 INCRBY 0 读（同 incrementAndGet 路径，不经值序列化）
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return 0;
        }
        Long count = redisTemplate.opsForValue().increment(key, 0);
        return count != null ? count : 0;
    }
}
