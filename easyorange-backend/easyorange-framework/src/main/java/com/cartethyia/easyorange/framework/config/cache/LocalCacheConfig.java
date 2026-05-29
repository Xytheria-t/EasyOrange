package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final CacheProperties cacheProperties;

    @Bean("imageProcessCache")
    public Cache<String, ImageProcessingCacheEntry> imageProcessCache() {
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

    public record ImageProcessingCacheEntry(File file, String mimeType, String eTag) {
    }
}
