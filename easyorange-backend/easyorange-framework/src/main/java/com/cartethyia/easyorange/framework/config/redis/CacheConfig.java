package com.cartethyia.easyorange.framework.config.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@AutoConfiguration
@EnableCaching
public class CacheConfig {

    private static final String PRODUCT_LIST_KEY = "eo:product:list:";
    private static final String CATEGORY_LIST_KEY = "eo:category:list";
    private static final long PRODUCT_LIST_EXPIRE_TIME = 30L;
    private static final long CATEGORY_INFO_EXPIRE_TIME = 120L;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                PRODUCT_LIST_KEY, defaultConfig.entryTtl(Duration.ofMinutes(PRODUCT_LIST_EXPIRE_TIME)),
                CATEGORY_LIST_KEY, defaultConfig.entryTtl(Duration.ofMinutes(CATEGORY_INFO_EXPIRE_TIME))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
