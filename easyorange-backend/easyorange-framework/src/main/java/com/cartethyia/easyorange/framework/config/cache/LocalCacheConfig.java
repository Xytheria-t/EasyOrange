package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.cache.CacheInvalidationListener;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.TimeUnit;

@Slf4j
@AutoConfiguration
@AutoConfigureAfter(org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class)
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final CacheProperties cacheProperties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final CacheInvalidationListener cacheInvalidationListener;

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
    public Cache<String, Object> imageProcessCache() {
        var imageProps = cacheProperties.getImage();
        return Caffeine.newBuilder()
                .maximumSize(imageProps.getMaxSize())
                .expireAfterAccess(imageProps.getExpireHours(), TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean("l1Cache")
    public Cache<String, Object> l1Cache() {
        var l1Props = cacheProperties.getL1();
        return Caffeine.newBuilder()
                .maximumSize(l1Props.getMaxSize())
                .expireAfterWrite(l1Props.getExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public MultiLevelCache multiLevelCache(@Qualifier("l1Cache") Cache<String, Object> l1Cache) {
        return new MultiLevelCache(
                l1Cache,
                redisTemplate,
                "mlc:",
                30,
                TimeUnit.MINUTES,
                cacheInvalidationListener);
    }

    /**
     * 注册 Redis Pub/Sub 监听容器 — 订阅 {@link CacheInvalidationListener#CHANNEL} 频道，
     * 收到失效消息后由 {@link CacheInvalidationListener} 失效本地 L1 缓存。
     * <p>
     * 这是跨节点 L1 缓存一致性的入口：节点 A 执行 {@link MultiLevelCache#evict(String)} →
     * 发布消息 → 节点 B 的本容器收到 → 调用 {@link CacheInvalidationListener#onMessage} →
     * 失效节点 B 的 L1 缓存。
     */
    @Bean
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
            RedisConnectionFactory connectionFactory) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                cacheInvalidationListener,
                new ChannelTopic(CacheInvalidationListener.CHANNEL));
        log.info("action=cache_invalidation_listener_registered, channel={}",
                CacheInvalidationListener.CHANNEL);
        return container;
    }
}
