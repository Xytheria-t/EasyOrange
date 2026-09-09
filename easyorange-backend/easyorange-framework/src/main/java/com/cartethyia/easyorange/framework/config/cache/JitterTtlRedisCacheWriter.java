package com.cartethyia.easyorange.framework.config.cache;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

/**
 * TTL 随机抖动装饰（防缓存雪崩）— put / store / putIfAbsent 写入时按比例给 TTL 加随机偏移，
 * 同批写入的 key 错峰过期；读与淘汰路径原样透传。
 */
class JitterTtlRedisCacheWriter implements RedisCacheWriter {

    private final RedisCacheWriter delegate;
    private final double jitterRatio;

    JitterTtlRedisCacheWriter(RedisCacheWriter delegate, double jitterRatio) {
        this.delegate = delegate;
        this.jitterRatio = Math.min(Math.max(jitterRatio, 0), 1);
    }

    @Override
    public void put(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
        delegate.put(name, key, value, jitter(ttl));
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
        return delegate.store(name, key, value, jitter(ttl));
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
        return delegate.putIfAbsent(name, key, value, jitter(ttl));
    }

    /** null（不过期）与非正数 TTL 不抖动；其余加 0~ratio 比例随机偏移。 */
    @Nullable
    private Duration jitter(@Nullable Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative() || jitterRatio <= 0) {
            return ttl;
        }
        return ttl.plusMillis(
                (long) (ttl.toMillis() * ThreadLocalRandom.current().nextDouble(jitterRatio)));
    }

    // —— 读写与统计路径透传 ——

    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return delegate.retrieve(name, key, ttl);
    }

    @Override
    public void evict(String name, byte[] key) {
        delegate.evict(name, key);
    }

    @Override
    public void clear(String name, byte[] pattern) {
        delegate.clear(name, pattern);
    }

    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    @Override
    public CacheStatistics getCacheStatistics(String name) {
        return delegate.getCacheStatistics(name);
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector statisticsCollector) {
        // 包一层保持抖动语义，避免统计收集器替换后丢失装饰
        return new JitterTtlRedisCacheWriter(delegate.withStatisticsCollector(statisticsCollector), jitterRatio);
    }
}
