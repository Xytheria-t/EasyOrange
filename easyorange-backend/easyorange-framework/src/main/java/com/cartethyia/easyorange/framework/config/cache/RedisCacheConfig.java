package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.config.redis.RedisConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 缓存配置 — Spring Cache 注解式 + Redis 单层；进程内本地缓存（如图片处理）见
 * {@link ImageProcessCacheConfig}。
 * <p>
 * 单层 Redis + 统一短 TTL（{@code easyorange.cache.default-ttl}）：一致性靠写路径显式 evict + TTL 兜底，
 * 不再需要 L1/L2 配平与跨节点广播；写入路径包 {@link JitterTtlRedisCacheWriter} 给 TTL 加随机抖动防雪崩。
 * <p>
 * 序列化复用 {@link RedisConfig} 的 {@link GenericJacksonJsonRedisSerializer}（JSON + 类型信息），值可读可调试。
 * <p>
 * 缓存故障由 {@link #errorHandler()} 集中 fail-open：读降级为直查 DB、写放弃本次缓存，业务侧无需逐点 try-catch。
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private final CacheProperties cacheProperties;
    /** fail-open 吞异常可以、吞统计不行：缓存故障计数进 Prometheus，日志只留排障细节。 */
    private final MeterRegistry meterRegistry;

    public RedisCacheConfig(CacheProperties cacheProperties, MeterRegistry meterRegistry) {
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Spring Cache 的 Redis 实现 — String key + JSON value（与 {@link RedisConfig} 序列化约定一致）。
     * Redis key 形如 {@code <cacheName>::<key>}（如 {@code eo:product:info::<productId>}）。
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory, GenericJacksonJsonRedisSerializer jsonRedisSerializer) {
        var defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.defaultTtl())
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonRedisSerializer));
        var cacheWriter = new JitterTtlRedisCacheWriter(
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), cacheProperties.ttlJitter());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .cacheWriter(cacheWriter)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "action=cache_get_failed, cache={}, key={}, error={}",
                        cache.getName(),
                        key,
                        exception.getMessage());
                recordFailure("get", cache);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn(
                        "action=cache_put_failed, cache={}, key={}, error={}",
                        cache.getName(),
                        key,
                        exception.getMessage());
                recordFailure("put", cache);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "action=cache_evict_failed, cache={}, key={}, error={}",
                        cache.getName(),
                        key,
                        exception.getMessage());
                recordFailure("evict", cache);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("action=cache_clear_failed, cache={}, error={}", cache.getName(), exception.getMessage());
                recordFailure("clear", cache);
            }

            private void recordFailure(String op, Cache cache) {
                meterRegistry
                        .counter("easyorange.cache.failures", "op", op, "cache", cache.getName())
                        .increment();
            }
        };
    }
}
