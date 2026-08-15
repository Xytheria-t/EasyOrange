package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 图片处理本地缓存配置 — Caffeine（进程内），独立于 Redis 缓存（{@link RedisCacheConfig}）。
 * <p>
 * 使用方需自行 cast 缓存值：
 * <pre>{@code
 * @SuppressWarnings("unchecked")
 * Cache<String, ImageProcessingCacheEntry> cache = (Cache<String, ImageProcessingCacheEntry>) imageProcessCache;
 * }</pre>
 */
@AutoConfiguration
public class ImageProcessCacheConfig {

    private final CacheProperties cacheProperties;

    public ImageProcessCacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

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
}
