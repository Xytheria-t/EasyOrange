package com.cartethyia.easyorange.framework.redis.impl;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.exception.CacheTypeMismatchException;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
public class RedisCacheImpl implements RedisCache {

    private static final String KEY_PREFIX = "${redis.key-prefix:}";

    private final RedisTemplate<String, Object> redisTemplate;

    @org.springframework.beans.factory.annotation.Value(KEY_PREFIX)
    private String keyPrefix;

    private static final String UNLOCK_SCRIPT = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT_INSTANCE;

    static {
        UNLOCK_SCRIPT_INSTANCE = new DefaultRedisScript<>();
        UNLOCK_SCRIPT_INSTANCE.setScriptText(UNLOCK_SCRIPT);
        UNLOCK_SCRIPT_INSTANCE.setResultType(Long.class);
    }

    private String generateKey(String key) {
        BizRequire.notBlank(key, "Key must not be null or empty");
        return keyPrefix.isEmpty() ? key : keyPrefix + ":" + key;
    }

    @SuppressWarnings("unchecked")
    private <T> T castValue(Object value, Class<T> type, String key) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof Number n && type != Number.class) {
            return switch (type) {
                case Class<?> t when t == Long.class -> (T) Long.valueOf(n.longValue());
                case Class<?> t when t == Integer.class -> (T) Integer.valueOf(n.intValue());
                case Class<?> t when t == Double.class -> (T) Double.valueOf(n.doubleValue());
                case Class<?> t when t == Float.class -> (T) Float.valueOf(n.floatValue());
                case Class<?> t when t == Short.class -> (T) Short.valueOf(n.shortValue());
                case Class<?> t when t == Byte.class -> (T) Byte.valueOf(n.byteValue());
                default -> throw new CacheTypeMismatchException(key, type, value.getClass());
            };
        }
        throw new CacheTypeMismatchException(key, type, value.getClass());
    }

    private <T> Set<T> filterByType(Set<?> candidates, Class<T> type) {
        if (candidates == null || candidates.isEmpty()) {
            return Set.of();
        }
        return candidates.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> multiGetInternal(Collection<String> keys, Class<T> type) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        List<String> rawKeys = keys.stream().map(this::generateKey).toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(rawKeys);
        if (values == null) {
            return Map.of();
        }
        List<String> keyList = keys instanceof List<String> l ? l : List.copyOf(keys);
        Map<String, T> result = new HashMap<>(keys.size());
        IntStream.range(0, values.size()).forEach(i -> {
            Object value = values.get(i);
            if (value != null) {
                result.put(keyList.get(i), type == null ? (T) value : type.cast(value));
            }
        });
        return result;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        return redisTemplate.expire(generateKey(key), timeout, timeUnit);
    }

    @Override
    public long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(generateKey(key), timeUnit);
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(generateKey(key));
    }

    @Override
    public boolean delete(String key) {
        return redisTemplate.delete(generateKey(key));
    }

    @Override
    public Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.delete(keys.stream().map(this::generateKey).toList());
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, -1, TimeUnit.SECONDS);
    }

    @Override
    public <T> void set(String key, T value, long timeout, TimeUnit timeUnit) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        String prefixedKey = generateKey(key);
        if (timeout > 0) {
            redisTemplate.opsForValue().set(prefixedKey, value, timeout, timeUnit);
        } else {
            redisTemplate.opsForValue().set(prefixedKey, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(generateKey(key));
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return castValue(redisTemplate.opsForValue().get(generateKey(key)), type, key);
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), value);
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), value, timeout, timeUnit);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(generateKey(key));
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(generateKey(key), delta);
    }

    @Override
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(generateKey(key));
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(generateKey(key), delta);
    }

    // ==================== Bitmap 操作 ====================

    @Override
    public Boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(generateKey(key), offset, value);
    }

    @Override
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(generateKey(key), offset);
    }

    @Override
    public Long bitCount(String key) {
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.bitCount(generateKey(key).getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public <T> Map<String, T> multiGet(Collection<String> keys) {
        return multiGetInternal(keys, null);
    }

    @Override
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> type) {
        return multiGetInternal(keys, type);
    }

    @Override
    public <T> void multiSet(Map<String, T> map) {
        BizRequire.notEmpty(map, "批量缓存数据不能为空");
        Map<String, Object> prefixedMap = new HashMap<>(map.size());
        map.forEach((k, v) -> prefixedMap.put(generateKey(k), v));
        redisTemplate.opsForValue().multiSet(prefixedMap);
    }

    @Override
    public Boolean tryLock(String key, String value, long timeout, TimeUnit timeUnit) {
        return setIfAbsent(key, value, timeout, timeUnit);
    }

    @Override
    public Boolean unlock(String key, Object value) {
        try {
            return redisTemplate.execute(
                    UNLOCK_SCRIPT_INSTANCE,
                    List.of(generateKey(key)),
                    value
            ) != null;
        } catch (Exception e) {
            log.error("action=redis_unlock, key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean unlockIfValueMatches(String key, String expectedValue) {
        return unlock(key, expectedValue);
    }

    @Override
    public <T> void hashPut(String key, String hashKey, T value) {
        redisTemplate.opsForHash().put(generateKey(key), hashKey, value);
    }

    @Override
    public <T> void hashPutAll(String key, Map<String, T> map) {
        BizRequire.notEmpty(map, "缓存数据不能为空");
        redisTemplate.opsForHash().putAll(generateKey(key), map);
    }

    @Override
    public Boolean hashPutIfAbsent(String key, String hashKey, Object value) {
        return redisTemplate.opsForHash().putIfAbsent(generateKey(key), hashKey, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(generateKey(key), hashKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hashGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(generateKey(key));
        Map<String, T> result = new HashMap<>(entries.size());
        entries.forEach((k, v) -> result.put(String.valueOf(k), (T) v));
        return result;
    }

    @Override
    public Boolean hashHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(generateKey(key), hashKey);
    }

    @Override
    public Long hashDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(generateKey(key), hashKeys);
    }

    @Override
    public Long hashSize(String key) {
        return redisTemplate.opsForHash().size(generateKey(key));
    }

    @Override
    public Long hashIncrement(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(generateKey(key), hashKey, delta);
    }

    @Override
    public <T> Long listPush(String key, T value) {
        return redisTemplate.opsForList().rightPush(generateKey(key), value);
    }

    @Override
    public <T> Long listPushLeft(String key, T value) {
        return redisTemplate.opsForList().leftPush(generateKey(key), value);
    }

    @Override
    public <T> T listPop(String key, Class<T> type) {
        return castValue(redisTemplate.opsForList().leftPop(generateKey(key)), type, key);
    }

    @Override
    public <T> T listPopRight(String key, Class<T> type) {
        return castValue(redisTemplate.opsForList().rightPop(generateKey(key)), type, key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> listRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(generateKey(key), start, end);
    }

    @Override
    public Long listSize(String key) {
        return redisTemplate.opsForList().size(generateKey(key));
    }

    @Override
    @SafeVarargs
    public final <T> Boolean setAdd(String key, T... values) {
        BizRequire.notEmpty(values, "添加的值不能为空");
        return redisTemplate.opsForSet().add(generateKey(key), values) != null;
    }

    @Override
    public <T> Set<T> setMembers(String key, Class<T> type) {
        return filterByType(redisTemplate.opsForSet().members(generateKey(key)), type);
    }

    @Override
    public Boolean setIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(generateKey(key), value);
    }

    @Override
    public Long setRemove(String key, Object... values) {
        BizRequire.notEmpty(values, "删除的值不能为空");
        return redisTemplate.opsForSet().remove(generateKey(key), values);
    }

    @Override
    public Long setSize(String key) {
        return redisTemplate.opsForSet().size(generateKey(key));
    }

    @Override
    public <T> Boolean zsetAdd(String key, double score, T value) {
        return redisTemplate.opsForZSet().add(generateKey(key), value, score);
    }

    @Override
    public <T> Set<T> zsetRangeByScore(String key, double min, double max, Class<T> type) {
        return filterByType(redisTemplate.opsForZSet().rangeByScore(generateKey(key), min, max), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> zsetRangeByScoreWithScores(String key, double min, double max) {
        return (Set<T>) redisTemplate.opsForZSet().rangeByScoreWithScores(generateKey(key), min, max);
    }

    @Override
    public Double zsetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(generateKey(key), value);
    }

    @Override
    public Long zsetRemove(String key, Object... values) {
        BizRequire.notEmpty(values, "删除的值不能为空");
        return redisTemplate.opsForZSet().remove(generateKey(key), values);
    }

    @Override
    public Long zsetSize(String key) {
        return redisTemplate.opsForZSet().size(generateKey(key));
    }

    @Override
    public Long executeLuaScript(DefaultRedisScript<Long> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    @Override
    public Set<String> keys(String pattern) {
        Set<String> rawKeys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysTmp = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    keysTmp.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                log.error("Redis SCAN failed for pattern: {}", pattern, e);
                throw new RuntimeException(e);
            }
            return keysTmp;
        });
        if (rawKeys == null || rawKeys.isEmpty()) {
            return Set.of();
        }
        return rawKeys.stream()
                .map(this::stripPrefix)
                .collect(Collectors.toSet());
    }

    private String stripPrefix(String key) {
        if (keyPrefix.isEmpty()) {
            return key;
        }
        String prefixWithColon = keyPrefix + ":";
        return key.startsWith(prefixWithColon) ? key.substring(prefixWithColon.length()) : key;
    }
}
