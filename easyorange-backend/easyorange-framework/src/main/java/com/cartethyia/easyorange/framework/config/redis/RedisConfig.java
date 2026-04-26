package com.cartethyia.easyorange.framework.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for customizing RedisTemplate serialization.
 * <p>
 * This configuration provides a RedisTemplate with JSON serialization support
 * using Jackson. It uses default type handling for polymorphic serialization.
 * </p>
 * <p>
 * Note: TTL (Time-To-Live) for Redis keys should be configured at the service layer
 * when storing data. This configuration does not set default TTL values.
 * </p>
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    /**
     * Creates and configures a RedisTemplate for storing string keys and JSON-serialized values.
     * <p>
     * Key serializers use StringRedisSerializer for human-readable keys.
     * Value serializers use RedisSerializer.json() for JSON serialization.
     * </p>
     *
     * @param connectionFactory the RedisConnectionFactory injected by Spring
     * @return configured RedisTemplate instance
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (human-readable)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use Jackson JSON serializer for values
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
