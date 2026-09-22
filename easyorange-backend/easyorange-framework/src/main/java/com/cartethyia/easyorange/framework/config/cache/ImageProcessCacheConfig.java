package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingCacheEntry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 图片处理本地缓存配置 — Caffeine（进程内），独立于 Redis 缓存（{@link RedisCacheConfig}）。
 * <p>
 * 值类型是 {@link ImageProcessingCacheEntry}：缓存 bean 的泛型因此唯一，注入点按类型即可解析，
 * 消费方（{@code ImageQueryService}）不必用 {@code @Qualifier} 字符串限定，也不必对读到的值强转。
 */
@AutoConfiguration
public class ImageProcessCacheConfig {

    private final CacheProperties cacheProperties;

    public ImageProcessCacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean("imageProcessCache")
    @ConditionalOnMissingBean(name = "imageProcessCache")
    public Cache<String, ImageProcessingCacheEntry> imageProcessCache() {
        var imageProps = cacheProperties.image();
        return Caffeine.newBuilder()
                .maximumSize(imageProps.maxSize())
                .expireAfterAccess(imageProps.expireHours(), TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
