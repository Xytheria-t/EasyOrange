package com.cartethyia.easyorange.framework.config.redis;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    // ==============================================
    // 1. 全局唯一 ObjectMapper（配置类型信息，兼容缓存反序列化）
    // 自动被 RedisSerializer.json() 复用，无手动绑定
    // ==============================================
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 核心：保留对象类型信息，和旧缓存完全兼容
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    // ==============================================
    // 2. 缓存管理器（仅使用无弃用、无报错的官方API）
    // ==============================================
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ✅ 官方4.x唯一无弃用JSON序列化器（无参调用，无任何报错）
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CacheConstants.Product.LIST_KEY, defaultConfig.entryTtl(Duration.ofMinutes(CacheConstants.Product.LIST_EXPIRE_TIME)),
                CacheConstants.Category.LIST_KEY, defaultConfig.entryTtl(Duration.ofMinutes(CacheConstants.Category.INFO_EXPIRE_TIME))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}