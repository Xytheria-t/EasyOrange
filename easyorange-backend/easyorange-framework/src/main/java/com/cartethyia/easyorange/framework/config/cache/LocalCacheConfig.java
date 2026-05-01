package com.cartethyia.easyorange.framework.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class LocalCacheConfig {

    @Value("${jwt.local-cache.max-size:10000}")
    private int tokenCacheMaxSize;

    @Value("${jwt.local-cache.expire-minutes:5}")
    private int tokenCacheExpireMinutes;

    @Bean("tokenUuidCache")
    public Cache<String, Boolean> tokenUuidCache() {
        return Caffeine.newBuilder()
                .maximumSize(tokenCacheMaxSize)
                .expireAfterWrite(tokenCacheExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
