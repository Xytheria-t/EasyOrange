package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.concurrent.TimeUnit;

public class MultiLevelCache {

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
            return (T) l1Value;
        }

        String l2Key = buildL2Key(key);
        T l2Value = redisCache.get(l2Key, type);
        if (l2Value != null) {
            l1Cache.put(key, l2Value);
            return l2Value;
        }

        T source = loader.load();
        if (source != null) {
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

    public void evictL2(String key) {
        redisCache.delete(buildL2Key(key));
    }

    private String buildL2Key(String key) {
        return l2KeyPrefix + key;
    }
}