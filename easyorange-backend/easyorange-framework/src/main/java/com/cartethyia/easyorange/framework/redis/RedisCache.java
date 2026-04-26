package com.cartethyia.easyorange.framework.redis;

import com.cartethyia.easyorange.common.util.BizRequire;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存操作封装
 * <p>
 * 提供 String、Hash、List、Set、ZSet 等常用操作，以及分布式锁和 Lua 脚本执行。
 * 所有 key 自动支持 prefix 前缀，便于多环境/多实例隔离。
 * </p>
 *
 * @author cartethyia
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
public class RedisCache {

    private static final String KEY_MUST_NOT_BE_NULL_OR_EMPTY = "Key must not be null or empty";
    private static final String VALUE_MUST_NOT_BE_NULL = "Value must not be null";
    private static final String TIMEOUT_MUST_BE_POSITIVE = "Timeout must be positive";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis key 前缀，通过 application.yml 的 redis.key-prefix 配置注入。
     * 多环境/多实例部署时用于 key 隔离（如 "dev:easyorange"、"prod:easyorange"）。
     */
    @org.springframework.beans.factory.annotation.Value("${redis.key-prefix:}")
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
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL_OR_EMPTY);
        }
        return keyPrefix.isEmpty() ? key : keyPrefix + ":" + key;
    }

    // ==================== String Operations ====================

    public <T> void set(String key, T value) {
        set(key, value, -1, TimeUnit.SECONDS);
    }

    public <T> void set(String key, T value, long timeout, TimeUnit timeUnit) {
        if (value == null) {
            throw new IllegalArgumentException(VALUE_MUST_NOT_BE_NULL);
        }
        if (timeout > 0) {
            redisTemplate.opsForValue().set(generateKey(key), value, timeout, timeUnit);
        } else {
            redisTemplate.opsForValue().set(generateKey(key), value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(generateKey(key));
    }

    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(generateKey(key));
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new ClassCastException("Value type mismatch: expected " + type.getName() + ", got " + value.getClass().getName());
    }

    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException(TIMEOUT_MUST_BE_POSITIVE);
        }
        return redisTemplate.expire(generateKey(key), timeout, timeUnit);
    }

    public long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(generateKey(key), timeUnit);
    }

    public long getExpire(String key) {
        return getExpire(key, TimeUnit.SECONDS);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(generateKey(key));
    }

    public boolean delete(String key) {
        return redisTemplate.delete(generateKey(key));
    }

    public boolean delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        return redisTemplate.delete(keys.stream().map(this::generateKey).toList()) > 0;
    }

    public <T> Boolean setIfAbsent(String key, T value) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), value);
    }

    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), value, timeout, timeUnit);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(generateKey(key));
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(generateKey(key), delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(generateKey(key));
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(generateKey(key), delta);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> prefixedKeys = keys.stream().map(this::generateKey).toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(prefixedKeys);
        if (values == null) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>(keys.size());
        Iterator<String> keyIterator = keys.iterator();
        for (Object value : values) {
            String key = keyIterator.next();
            if (value != null) {
                result.put(key, (T) value);
            }
        }
        return result;
    }

    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> type) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> prefixedKeys = keys.stream().map(this::generateKey).toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(prefixedKeys);
        if (values == null) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>(keys.size());
        Iterator<String> keyIterator = keys.iterator();
        for (Object value : values) {
            String key = keyIterator.next();
            if (value != null && type.isInstance(value)) {
                result.put(key, type.cast(value));
            }
        }
        return result;
    }

    public <T> void multiSet(Map<String, T> map) {
        BizRequire.notEmpty(map, "批量缓存数据不能为空");
        Map<String, Object> prefixedMap = new HashMap<>();
        map.forEach((key, value) -> prefixedMap.put(generateKey(key), value));
        redisTemplate.opsForValue().multiSet(prefixedMap);
    }

    public Long multiDelete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.delete(keys.stream().map(this::generateKey).toList());
    }

    // ==================== Distributed Lock ====================

    /**
     * 简易分布式锁（基于 SETNX）
     * <p>
     * 注意：此实现不支持看门狗续期，适用于短时效的幂等保护场景。
     * 如需完整的分布式锁功能，建议集成 Redisson。
     * </p>
     */
    public <T> Boolean tryLock(String key, T value, long timeout, TimeUnit timeUnit) {
        return setIfAbsent(key, value, timeout, timeUnit);
    }

    public Boolean unlock(String key, Object value) {
        try {
            return redisTemplate.execute(
                UNLOCK_SCRIPT_INSTANCE,
                Collections.singletonList(generateKey(key)),
                value
            ) != null;
        } catch (Exception e) {
            log.error("action=redis_unlock, key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    // ==================== Hash Operations ====================

    public <T> void hashPut(String key, String hashKey, T value) {
        redisTemplate.opsForHash().put(generateKey(key), hashKey, value);
    }

    public <T> void hashPutAll(String key, Map<String, T> map) {
        BizRequire.notEmpty(map, "缓存数据不能为空");
        Map<String, Object> convertedMap = new HashMap<>();
        map.forEach(convertedMap::put);
        redisTemplate.opsForHash().putAll(generateKey(key), convertedMap);
    }

    @SuppressWarnings("unchecked")
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(generateKey(key), hashKey);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hashGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(generateKey(key));
        Map<String, T> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(String.valueOf(k), (T) v));
        return result;
    }

    public Boolean hashHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(generateKey(key), hashKey);
    }

    public Long hashDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(generateKey(key), hashKeys);
    }

    public Long hashSize(String key) {
        return redisTemplate.opsForHash().size(generateKey(key));
    }

    // ==================== List Operations ====================

    public <T> Long listPush(String key, T value) {
        return redisTemplate.opsForList().rightPush(generateKey(key), value);
    }

    public <T> Long listPushLeft(String key, T value) {
        return redisTemplate.opsForList().leftPush(generateKey(key), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T listPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(generateKey(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T listPopRight(String key) {
        return (T) redisTemplate.opsForList().rightPop(generateKey(key));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> listRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(generateKey(key), start, end);
    }

    public Long listSize(String key) {
        return redisTemplate.opsForList().size(generateKey(key));
    }

    // ==================== Set Operations ====================

    @SafeVarargs
    public final <T> Boolean setAdd(String key, T... values) {
        BizRequire.notEmpty(values, "添加的值不能为空");
        return redisTemplate.opsForSet().add(generateKey(key), values) != null;
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> setMembers(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(generateKey(key));
    }

    public <T> Boolean setIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(generateKey(key), value);
    }

    public Long setRemove(String key, Object... values) {
        BizRequire.notEmpty(values, "删除的值不能为空");
        return redisTemplate.opsForSet().remove(generateKey(key), values);
    }

    public Long setSize(String key) {
        return redisTemplate.opsForSet().size(generateKey(key));
    }

    // ==================== ZSet Operations ====================

    public <T> Boolean zsetAdd(String key, double score, T value) {
        return redisTemplate.opsForZSet().add(generateKey(key), value, score);
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> zsetRangeByScore(String key, double min, double max) {
        return (Set<T>) redisTemplate.opsForZSet().rangeByScore(generateKey(key), min, max);
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> zsetRangeByScoreWithScores(String key, double min, double max) {
        return (Set<T>) redisTemplate.opsForZSet().rangeByScoreWithScores(generateKey(key), min, max);
    }

    public Double zsetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(generateKey(key), value);
    }

    public Long zsetRemove(String key, Object... values) {
        BizRequire.notEmpty(values, "删除的值不能为空");
        return redisTemplate.opsForZSet().remove(generateKey(key), values);
    }

    public Long zsetSize(String key) {
        return redisTemplate.opsForZSet().size(generateKey(key));
    }

    // ==================== Lua Script ====================

    public Long executeLuaScript(DefaultRedisScript<Long> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }
}
