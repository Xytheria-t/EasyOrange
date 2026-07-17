package com.cartethyia.easyorange.framework.config.cache;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.framework.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final CacheProperties cacheProperties;
    private final RedisTemplate<String, Object> redisTemplate;

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
