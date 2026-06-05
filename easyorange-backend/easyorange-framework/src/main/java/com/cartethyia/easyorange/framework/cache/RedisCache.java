package com.cartethyia.easyorange.framework.cache;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.exception.CacheTypeMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.NumberUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.key-prefix:}")
    private String keyPrefix;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        var s = new DefaultRedisScript<Long>();
        s.setScriptText("""
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
                """);
        s.setResultType(Long.class);
        UNLOCK_SCRIPT = s;
    }

    // ==================== Key prefix ====================

    private String key(String raw) {
        BizRequire.notBlank(raw, "Key不能为空");
        return keyPrefix.isEmpty() ? raw : keyPrefix + ":" + raw;
    }

    private String stripPrefix(String k) {
        if (keyPrefix.isEmpty()) return k;
        var p = keyPrefix + ":";
        return k.startsWith(p) ? k.substring(p.length()) : k;
    }

    // ==================== KV ====================

    public void set(String key, Object value) {
        set(key, value, -1, TimeUnit.SECONDS);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) throw new IllegalArgumentException("Value must not be null");
        var k = key(key);
        if (timeout > 0) {
            redisTemplate.opsForValue().set(k, value, timeout, unit);
        } else {
            redisTemplate.opsForValue().set(k, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key(key));
    }

    public <T> T get(String key, Class<T> type) {
        return cast(redisTemplate.opsForValue().get(key(key)), type, key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key(key));
    }

    public Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return 0L;
        return redisTemplate.delete(keys.stream().map(this::key).toList());
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key(key));
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (timeout <= 0) throw new IllegalArgumentException("Timeout must be positive");
        return redisTemplate.expire(key(key), timeout, unit);
    }

    public long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key(key), unit);
    }

    // ==================== Atomic ====================

    public Long increment(String key) {
        return increment(key, 1);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key(key), delta);
    }

    // ==================== NX ====================

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key(key), value, timeout, unit);
    }

    // ==================== Lock ====================

    public Boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        return setIfAbsent(key, value, timeout, unit);
    }

    public Boolean unlock(String key, Object value) {
        try {
            return redisTemplate.execute(UNLOCK_SCRIPT, List.of(key(key)), value) != null;
        } catch (Exception e) {
            log.error("action=redis_unlock, key={}", key, e);
            return false;
        }
    }

    // ==================== Hash ====================

    public void hashPutAll(String key, Map<String, ?> map) {
        redisTemplate.opsForHash().putAll(key(key), map);
    }

    // ==================== Lua ====================

    public Long executeLuaScript(DefaultRedisScript<Long> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    // ==================== SCAN ====================

    public Set<String> keys(String pattern) {
        Set<String> raw = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> r = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.keyCommands()
                    .scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) r.add(new String(cursor.next(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Redis SCAN failed for pattern: {}", pattern, e);
                throw new RuntimeException(e);
            }
            return r;
        });
        if (raw == null || raw.isEmpty()) return Set.of();
        return raw.stream().map(this::stripPrefix).collect(Collectors.toSet());
    }

    // ==================== Internal ====================

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value, Class<T> type, String key) {
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);
        if (value instanceof Number n) {
            try {
                return type.cast(NumberUtils.convertNumberToTargetClass(n, (Class<? extends Number>) type));
            } catch (IllegalArgumentException e) {
                throw new CacheTypeMismatchException(key, type, value.getClass());
            }
        }
        throw new CacheTypeMismatchException(key, type, value.getClass());
    }
}
