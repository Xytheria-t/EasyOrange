package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@AutoConfigureAfter(org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class)
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final CacheProperties cacheProperties;
    private final RedisTemplate<Object, Object> redisTemplate;

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
        return new MultiLevelCache(l1Cache, redisTemplate);
    }
}
