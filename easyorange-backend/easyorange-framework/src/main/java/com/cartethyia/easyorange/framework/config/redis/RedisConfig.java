package com.cartethyia.easyorange.framework.config.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis 序列化器配置。
 * <p>
 * Spring Boot 4 的 {@link DataRedisAutoConfiguration} 自动配置的 {@code RedisTemplate}
 * <b>不设置任何序列化器</b>，默认使用 {@code JdkSerializationRedisSerializer}，导致：
 * <ul>
 *   <li>key/value 为二进制，Redis CLI 不可读、不可调试</li>
 *   <li>Lua 脚本的 ARGV 参数被序列化成二进制，{@code tonumber()} 返回 nil → 限流器 fail-open</li>
 *   <li>JDK 反序列化存在安全风险</li>
 * </ul>
 * <p>
 * 本配置统一设置：
 * <ul>
 *   <li>key / hashKey：{@link StringRedisSerializer}（UTF-8 可读）</li>
 *   <li>value / hashValue：{@link GenericJacksonJsonRedisSerializer}（Jackson 3 JSON + 默认类型信息）</li>
 * </ul>
 * 通过 {@code @AutoConfigureBefore} 确保本 Bean 先于自动配置注册，
 * 触发 {@code @ConditionalOnMissingBean} 跳过默认实现。
 */
@AutoConfiguration
@AutoConfigureBefore(DataRedisAutoConfiguration.class)
public class RedisConfig {

    /** JSON 序列化器 — RedisTemplate 与 Spring Cache（{@code RedisCacheConfig}）共用，保证序列化约定一致。 */
    @Bean
    public GenericJacksonJsonRedisSerializer jsonRedisSerializer() {
        return buildJsonSerializer();
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(
            RedisConnectionFactory connectionFactory, GenericJacksonJsonRedisSerializer jsonRedisSerializer) {
        var stringSerializer = StringRedisSerializer.UTF_8;

        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        return template;
    }

    private static GenericJacksonJsonRedisSerializer buildJsonSerializer() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
    }
}
