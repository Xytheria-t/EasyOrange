package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.interceptor.AiRateLimitInterceptor;
import com.cartethyia.easyorange.framework.cache.CacheInvalidationListener;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiCacheConfig implements WebMvcConfigurer {

    private final AiProperties aiProperties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final AiRateLimitInterceptor aiRateLimitInterceptor;
    private final CacheInvalidationListener cacheInvalidationListener;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Bean("aiCaches")
    public Map<AiCallScope, MultiLevelCache> aiCaches() {
        var props = aiProperties.getCache();
        var map = new EnumMap<AiCallScope, MultiLevelCache>(AiCallScope.class);

        for (var scope : AiCallScope.values()) {
            Cache<String, Object> l1 = Caffeine.newBuilder()
                    .maximumSize(props.getL1MaxSize())
                    .expireAfterWrite(props.getL1ExpireMinutes(), TimeUnit.MINUTES)
                    .build();

            var config = MultiLevelCache.Config.of(
                    scope.cacheKeyPrefix(),
                    Duration.ofMinutes(props.getL1ExpireMinutes()),
                    Duration.ofSeconds(scope.getTtlSeconds()),
                    null);

            var mlc = new MultiLevelCache(
                    l1,
                    redisTemplate,
                    config,
                    cacheInvalidationListener,
                    redissonClientProvider.getIfAvailable(),
                    meterRegistryProvider.getIfAvailable()
            );
            map.put(scope, mlc);
        }

        log.info("AI caches initialized: {} scopes, L1 maxSize={}, L1 expire={}min, cross-node invalidation=on",
                map.size(), props.getL1MaxSize(), props.getL1ExpireMinutes());
        return map;
    }



    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (aiProperties.getRateLimit().isEnabled()) {
            registry.addInterceptor(aiRateLimitInterceptor)
                    .addPathPatterns("/api/ai/**")
                    .order(0);
            log.info("AI rate limit interceptor registered for /api/ai/**");
        }
    }
}
