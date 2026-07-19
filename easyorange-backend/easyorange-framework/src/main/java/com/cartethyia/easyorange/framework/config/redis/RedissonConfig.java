package com.cartethyia.easyorange.framework.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Redisson 独立配置（不使用 Spring Boot Starter，避免与现有 RedisTemplate 冲突）。
 * <p>
 * 从标准 {@link DataRedisProperties} 读取连接信息，与 Spring Data Redis 共享同一 Redis 实例。
 * 适用于分布式锁（RLock 实现 {@link java.util.concurrent.locks.Lock} 接口 + Watch Dog 自动续期）。
 * </p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(DataRedisProperties.class)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(DataRedisProperties properties) {
        Config config = new Config();

        String host = properties.getHost() != null ? properties.getHost() : "localhost";
        int port = properties.getPort() > 0 ? properties.getPort() : 6379;
        String password = properties.getPassword();
        int database = properties.getDatabase();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(4);

        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }

        Duration timeout = properties.getTimeout();
        if (timeout != null) {
            serverConfig.setConnectTimeout((int) timeout.toMillis());
        }

        log.info("RedissonClient initialized: redis://{}:{}/{}", host, port, database);
        return Redisson.create(config);
    }
}
