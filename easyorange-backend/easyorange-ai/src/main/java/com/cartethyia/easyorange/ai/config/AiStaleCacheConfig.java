package com.cartethyia.easyorange.ai.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiStaleCacheConfig {

    private final AiProperties aiProperties;

    @Bean("aiStaleCache")
    public Cache<String, Object> aiStaleCache() {
        var props = aiProperties.cache();
        log.info("AI stale cache initialized: maxSize={}, expire={}h", props.staleMaxSize(), props.staleExpireHours());
        return Caffeine.newBuilder()
                .maximumSize(props.staleMaxSize())
                .expireAfterWrite(props.staleExpireHours(), TimeUnit.HOURS)
                .build();
    }
}
