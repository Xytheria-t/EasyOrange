package com.cartethyia.easyorange.framework.cache;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MultiLevelCache {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelCache.class);

    private final Cache<String, Object> l1Cache;
    private final RedisCache redisCache;
    private final String l2KeyPrefix;
    private final long l2DefaultTimeout;
    private final TimeUnit l2DefaultUnit;

    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisCache redisCache) {
        this(l1Cache, redisCache, "mlc:", 30, TimeUnit.MINUTES);
    }

    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisCache redisCache,
            String l2KeyPrefix,
            long l2DefaultTimeout,
            TimeUnit l2DefaultUnit) {
        this.l1Cache = l1Cache;
        this.redisCache = redisCache;
        this.l2KeyPrefix = l2KeyPrefix;
        this.l2DefaultTimeout = l2DefaultTimeout;
        this.l2DefaultUnit = l2DefaultUnit;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, CacheLoader<T> loader) {
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            log.trace("action=mlc_hit_l1 key={}", key);
            return (T) l1Value;
        }

        String l2Key = buildL2Key(key);
        T l2Value = redisCache.get(l2Key, type);
        if (l2Value != null) {
            log.trace("action=mlc_hit_l2 key={}", key);
            l1Cache.put(key, l2Value);
            return l2Value;
        }

        T source = loader.load();
        if (source != null) {
            log.trace("action=mlc_miss_load key={}", key);
            redisCache.set(l2Key, source, l2DefaultTimeout, l2DefaultUnit);
            l1Cache.put(key, source);
        }
        return source;
    }

    public void evict(String key) {
        l1Cache.invalidate(key);
        redisCache.delete(buildL2Key(key));
    }

    public <T> void put(String key, T value) {
        if (key == null || value == null) {
            return;
        }
        l1Cache.put(key, value);
        redisCache.set(buildL2Key(key), value, l2DefaultTimeout, l2DefaultUnit);
    }

    public void evictL1(String key) {
        l1Cache.invalidate(key);
    }

    public void evictL2(String key) {
        redisCache.delete(buildL2Key(key));
    }

    public void clear() {
        l1Cache.invalidateAll();
    }

    public Cache<String, Object> getL1Cache() {
        return l1Cache;
    }

    private String buildL2Key(String key) {
        return l2KeyPrefix + key;
    }
}