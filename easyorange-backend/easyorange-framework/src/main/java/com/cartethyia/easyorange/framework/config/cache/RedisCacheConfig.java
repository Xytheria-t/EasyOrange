package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.config.redis.RedisConfig;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
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
import org.springframework.data.redis.cache.BatchStrategies;
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
 * 不再需要 L1/L2 配平与跨节点广播；写入路径包 {@link JitterTtlRedisCacheWriter} 给 TTL 加随机抖动防雪崩；
 * 读路径用 SDR 的 **locking writer** 承载 {@code @Cacheable(sync = true)} 的单飞（防击穿）——
 * 单飞不是注解自己做的，是 writer 做的，换成 non-locking 就是静默失效（见 {@link JitterTtlRedisCacheWriter#get}）。
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

    /**
     * 锁重试间隔。SDR 的默认值也是 50ms —— 单飞只在 {@code sync = true} 的未命中路径上发生
     * （全仓仅商品详情一处），等待成本可忽略。
     */
    private static final Duration LOCK_SLEEP = Duration.ofMillis(50);

    /**
     * 锁 TTL。必须给：{@code lockingRedisCacheWriter} 的默认是 {@code persistent()}（永不过期），
     * 持有者进程崩在 loader 里就会把这个 key 永久锁死。10s 远高于一次本地 DB 查询，又短到崩了能自愈；
     * 万一 loader 超过它，退化成「并发各跑一次」，也就是加锁前的行为，不会错只是慢。
     */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

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
                RedisCacheWriter.lockingRedisCacheWriter(
                        connectionFactory,
                        LOCK_SLEEP,
                        RedisCacheWriter.TtlFunction.just(LOCK_TTL),
                        BatchStrategies.keys()),
                cacheProperties.ttlJitter());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .cacheWriter(cacheWriter)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        // 缓存故障一律降级为日志 + 失败计数，绝不抛给业务方：Redis 抖动不该让下单失败
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                fail("get", cache, key, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                fail("put", cache, key, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                fail("evict", cache, key, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                fail("clear", cache, null, e);
            }

            private void fail(String op, Cache cache, Object key, RuntimeException e) {
                log.warn(
                        "action=cache_{}_failed, cache={}, key={}, error={}", op, cache.getName(), key, e.getMessage());
                meterRegistry
                        .counter("easyorange.cache.failures", "op", op, "cache", cache.getName())
                        .increment();
            }
        };
    }
}
