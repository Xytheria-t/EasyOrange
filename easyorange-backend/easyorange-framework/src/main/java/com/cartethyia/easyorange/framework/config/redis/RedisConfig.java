package com.cartethyia.easyorange.framework.config.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis 配置（仅自定义序列化器，RedisTemplate 由 Spring Boot 自动配置）
 * <p>
 * Spring Boot 4 的 {@link DataRedisAutoConfiguration} 已自动配置 Jackson 3 的
 * {@link GenericJacksonJsonRedisSerializer}。本配置类仅作为扩展点保留，
 * 如需自定义序列化行为可在此添加 {@code RedisTemplateCustomizer}。
 */
@AutoConfiguration
public class RedisConfig {

    // Spring Boot 4 自动配置的 redisTemplate 已使用 Jackson 3 GenericJacksonJsonRedisSerializer
    // 无需自定义 Bean，直接注入 RedisTemplate<String, Object> 即可
}
