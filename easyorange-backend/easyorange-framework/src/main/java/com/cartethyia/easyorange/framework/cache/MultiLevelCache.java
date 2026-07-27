package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存 — L1（Caffeine 本地）+ L2（Redis 共享）。
 * <p>
 * 跨节点 L1 一致性：当本节点执行 {@link #evict(String)} / {@link #evictL2(String)} / {@link #put(String, Object)}
 * 时，通过 {@link CacheInvalidationListener} 发布失效消息到 Redis Pub/Sub，其他节点收到消息后失效本地 L1。
 * <p>
 * 调用 {@link #get(String, Class, CacheLoader)} 触发的回源填充<b>不</b>发布失效消息——
 * 因为回源填充的是新值，不涉及其他节点 L1 的陈旧数据。
 */
public class MultiLevelCache {

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final String l2KeyPrefix;
    private final long l2DefaultTimeout;
    private final TimeUnit l2DefaultUnit;
    /** 跨节点 L1 失效广播，可为 null（测试场景或单节点部署） */
    private final CacheInvalidationListener invalidationListener;

    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisTemplate<Object, Object> redisTemplate) {
        this(l1Cache, redisTemplate, "mlc:", 30, TimeUnit.MINUTES, null);
    }

    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisTemplate<Object, Object> redisTemplate,
            String l2KeyPrefix,
            long l2DefaultTimeout,
            TimeUnit l2DefaultUnit) {
        this(l1Cache, redisTemplate, l2KeyPrefix, l2DefaultTimeout, l2DefaultUnit, null);
    }

    /**
     * 完整构造器 — 支持跨节点 L1 失效广播。
     *
     * @param invalidationListener 跨节点失效广播器，null 表示不参与广播（单节点 / 测试场景）
     */
    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisTemplate<Object, Object> redisTemplate,
            String l2KeyPrefix,
            long l2DefaultTimeout,
            TimeUnit l2DefaultUnit,
            CacheInvalidationListener invalidationListener) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.l2KeyPrefix = l2KeyPrefix;
        this.l2DefaultTimeout = l2DefaultTimeout;
        this.l2DefaultUnit = l2DefaultUnit;
        this.invalidationListener = invalidationListener;
        if (invalidationListener != null) {
            invalidationListener.register(l2KeyPrefix, l1Cache);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, CacheLoader<T> loader) {
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            return (T) l1Value;
        }

        String l2Key = buildL2Key(key);
        T l2Value = CacheUtils.cast(redisTemplate.opsForValue().get(l2Key), type);
        if (l2Value != null) {
            l1Cache.put(key, l2Value);
            return l2Value;
        }

        T source = loader.load();
        if (source != null) {
            redisTemplate.opsForValue().set(l2Key, source, l2DefaultTimeout, l2DefaultUnit);
            l1Cache.put(key, source);
        }
        return source;
    }

    /**
     * 失效 L1 + L2，并广播到其他节点失效它们的 L1。
     */
    public void evict(String key) {
        l1Cache.invalidate(key);
        redisTemplate.delete(buildL2Key(key));
        publishInvalidation(key);
    }

    /**
     * 显式写入 L1 + L2，并广播到其他节点失效它们的 L1（避免其他节点继续返回陈旧值）。
     */
    public <T> void put(String key, T value) {
        if (key == null || value == null) {
            return;
        }
        l1Cache.put(key, value);
        redisTemplate.opsForValue().set(buildL2Key(key), value, l2DefaultTimeout, l2DefaultUnit);
        publishInvalidation(key);
    }

    /**
     * 仅失效 L2，并广播到其他节点失效它们的 L1。
     * <p>
     * 用于列表类缓存（无法精确失效 L1，但 L2 已删除时其他节点的 L1 副本必然陈旧）。
     */
    public void evictL2(String key) {
        redisTemplate.delete(buildL2Key(key));
        publishInvalidation(key);
    }

    private void publishInvalidation(String key) {
        if (invalidationListener != null) {
            invalidationListener.publishInvalidation(l2KeyPrefix, key);
        }
    }

    private String buildL2Key(String key) {
        return l2KeyPrefix + key;
    }
}
