package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.config.redis.RedisConfig;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 缓存配置 — Spring Cache 注解式 + Redis 单层（替代已移除的手写多级缓存）。
 * <p>
 * 设计要点：
 * <ul>
 *   <li><b>注解驱动</b>：业务侧用 {@code @Cacheable}/{@code @CacheEvict}，零缓存代码；</li>
 *   <li><b>单层 Redis + 统一短 TTL</b>：TTL 由 {@code easyorange.cache.default-ttl} 控制，
 *       一致性靠写路径显式 evict + TTL 兜底，不再需要 L1/L2 配平与跨节点广播；</li>
 *   <li><b>序列化与 RedisTemplate 一致</b>：复用 {@link RedisConfig} 的
 *       {@link GenericJacksonJsonRedisSerializer}（JSON + 类型信息），值可读可调试；</li>
 *   <li><b>缓存故障 fail-open</b>：{@link #errorHandler()} 集中吞掉 Redis 异常并降级
 *       （读 → 直查 DB；写 → 放弃本次缓存），替代旧逐点 try-catch。</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    public RedisCacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 图片处理缓存（通用 Object 类型，适配多种缓存值类型）
     * <p>
     * 使用方需自行 cast 缓存值：
     * <pre>{@code
     * @SuppressWarnings("unchecked")
     * Cache<String, ImageProcessingCacheEntry> cache = (Cache<String, ImageProcessingCacheEntry>) imageProcessCache;
     * }</pre>
     */
    @Bean("imageProcessCache")
    @ConditionalOnMissingBean(name = "imageProcessCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> imageProcessCache() {
        var imageProps = cacheProperties.getImage();
        return Caffeine.newBuilder()
                .maximumSize(imageProps.getMaxSize())
                .expireAfterAccess(imageProps.getExpireHours(), TimeUnit.HOURS)
                .recordStats()
                .build();
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
                .entryTtl(cacheProperties.getDefaultTtl())
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonRedisSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }

    /**
     * 缓存故障 fail-open — Redis 异常统一吞掉并降级（读 → 直查 DB；写 → 放弃本次缓存），
     * 与旧 {@code MultiLevelCache} 的逐点 try-catch 语义一致，但集中一处、注解侧零改动。
     */
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
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn(
                        "action=cache_put_failed, cache={}, key={}, error={}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "action=cache_evict_failed, cache={}, key={}, error={}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("action=cache_clear_failed, cache={}, error={}", cache.getName(), exception.getMessage());
            }
        };
    }
}
