package com.cartethyia.easyorange.framework.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Configuration
public class LocalCacheConfig {

    @Value("${jwt.local-cache.max-size:10000}")
    private int tokenCacheMaxSize;

    @Value("${jwt.local-cache.expire-minutes:5}")
    private int tokenCacheExpireMinutes;

    @Value("${image.cache.max-size:1000}")
    private int imageCacheMaxSize;

    @Value("${image.cache.expire-hours:24}")
    private int imageCacheExpireHours;

    @Value("${multi-level-cache.l1.max-size:5000}")
    private int l1CacheMaxSize;

    @Value("${multi-level-cache.l1.expire-minutes:10}")
    private int l1CacheExpireMinutes;

    @Bean("tokenUuidCache")
    public Cache<String, Boolean> tokenUuidCache() {
        return Caffeine.newBuilder()
                .maximumSize(tokenCacheMaxSize)
                .expireAfterWrite(tokenCacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean("imageProcessCache")
    public Cache<String, ImageProcessingCacheEntry> imageProcessCache() {
        return Caffeine.newBuilder()
                .maximumSize(imageCacheMaxSize)
                .expireAfterAccess(imageCacheExpireHours, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean("l1Cache")
    public Cache<String, Object> l1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(l1CacheMaxSize)
                .expireAfterWrite(l1CacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public record ImageProcessingCacheEntry(File file, String mimeType, String eTag) {
    }
}
