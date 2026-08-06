package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.cache.CacheInvalidationListener;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.framework.config.async.MdcTaskDecorator;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@AutoConfiguration
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final CacheProperties cacheProperties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final CacheInvalidationListener cacheInvalidationListener;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

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
    public Cache<String, Object> imageProcessCache() {
        var imageProps = cacheProperties.getImage();
        return Caffeine.newBuilder()
                .maximumSize(imageProps.getMaxSize())
                .expireAfterAccess(imageProps.getExpireHours(), TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean("l1Cache")
    @ConditionalOnMissingBean(name = "l1Cache")
    public Cache<String, Object> l1Cache() {
        var l1Props = cacheProperties.getL1();
        return Caffeine.newBuilder()
                .maximumSize(l1Props.getMaxSize())
                .expireAfterWrite(l1Props.getExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(MultiLevelCache.class)
    public MultiLevelCache multiLevelCache(@Qualifier("l1Cache") Cache<String, Object> l1Cache) {
        var l1Props = cacheProperties.getL1();
        var l2Props = cacheProperties.getL2();
        var config = MultiLevelCache.Config.of(
                "mlc:",
                Duration.ofMinutes(l1Props.getExpireMinutes()),
                Duration.ofMinutes(l2Props.getExpireMinutes()),
                Duration.ofSeconds(l2Props.getNegativeExpireSeconds()));
        return new MultiLevelCache(
                l1Cache,
                redisTemplate,
                config,
                cacheInvalidationListener,
                redissonClientProvider.getIfAvailable(),
                meterRegistryProvider.getIfAvailable());
    }

    /** 失效消息分发线程池 — 量小但需有界，避免 SimpleAsyncTaskExecutor 无界线程。 */
    @Bean("cacheInvalidationTaskExecutor")
    @ConditionalOnMissingBean(name = "cacheInvalidationTaskExecutor")
    public ThreadPoolTaskExecutor cacheInvalidationTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-invalidation-");
        // 同步主线程 MDC（traceId 等），保证失效处理日志跨线程可追踪
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 注册 Redis Pub/Sub 监听容器 — 订阅 {@link CacheInvalidationListener#CHANNEL} 频道，
     * 收到失效消息后由 {@link CacheInvalidationListener} 失效本地 L1 缓存。
     * <p>
     * 这是跨节点 L1 缓存一致性的入口：节点 A 执行 {@link MultiLevelCache#evict(String)} →
     * 发布消息 → 节点 B 的本容器收到 → 调用 {@link CacheInvalidationListener#handleMessage} →
     * 失效节点 B 的 L1 缓存。
     */
    @Bean
    @ConditionalOnMissingBean(name = "cacheInvalidationListenerContainer")
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("cacheInvalidationTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(taskExecutor);
        container.addMessageListener(
                (Message message, byte[] _) ->
                        cacheInvalidationListener.handleMessage(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(CacheInvalidationListener.CHANNEL));
        log.info("action=cache_invalidation_listener_registered, channel={}", CacheInvalidationListener.CHANNEL);
        return container;
    }
}
