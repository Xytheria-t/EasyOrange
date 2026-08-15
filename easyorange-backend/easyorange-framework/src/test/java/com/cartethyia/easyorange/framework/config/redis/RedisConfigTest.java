package com.cartethyia.easyorange.framework.config.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig 序列化器配置守卫测试。
 * <p>
 * 防止 Spring Boot 4 {@code DataRedisAutoConfiguration} 的"零序列化器"默认行为回归：
 * 如果有人移除 {@link RedisConfig} 的序列化器配置，或改回 JDK 序列化，
 * 以下测试会立即失败 — 避免重蹈"Lua ARGV 变二进制 → 限流 fail-open"的覆辙。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig 序列化器配置守卫")
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Test
    @DisplayName("key/hashKey 使用 StringRedisSerializer（UTF-8 可读，Lua 兼容）")
    void redisTemplate_keyAndHashKey_useStringSerializer() {
        var config = new RedisConfig();
        RedisTemplate<Object, Object> template = config.redisTemplate(connectionFactory, config.jsonRedisSerializer());

        assertThat(template.getKeySerializer())
                .as("key 序列化器必须是 StringRedisSerializer，否则 Lua tonumber(ARGV) 返回 nil")
                .isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("value/hashValue 使用 GenericJacksonJsonRedisSerializer（JSON + 类型信息）")
    void redisTemplate_valueAndHashValue_useJsonSerializer() {
        var config = new RedisConfig();
        RedisTemplate<Object, Object> template = config.redisTemplate(connectionFactory, config.jsonRedisSerializer());

        assertThat(template.getValueSerializer())
                .as("value 序列化器必须是 GenericJacksonJsonRedisSerializer，否则 Redis CLI 不可读")
                .isInstanceOf(GenericJacksonJsonRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(GenericJacksonJsonRedisSerializer.class);
    }

    @Test
    @DisplayName("connectionFactory 正确注入")
    void redisTemplate_connectionFactory_set() {
        var config = new RedisConfig();
        RedisTemplate<Object, Object> template = config.redisTemplate(connectionFactory, config.jsonRedisSerializer());

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
    }
}
