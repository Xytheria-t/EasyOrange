package com.cartethyia.easyorange.framework.config.redis;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 独立配置 — 手写客户端而非 {@code redisson-spring-boot-starter}：
 * starter 自动配置会注册 {@code RedissonConnectionFactory} + {@code RedisTemplate}，
 * 与 Boot 默认 Lettuce 连接栈同为 {@code @ConditionalOnMissingBean} 竞争，可能整体接管
 * 应用的 Redis 连接；本项目只需 RLock，不换连接栈。连接信息复用标准
 * {@link DataRedisProperties}，与 Spring Data Redis 共享同一 Redis 实例。
 * <p>
 * watchdog 续期语义（leaseTime=-1 的锁、长持有告警）见
 * {@link com.cartethyia.easyorange.framework.lock.DistributedRedissonLockAdapter}。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(DataRedisProperties.class)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(DataRedisProperties properties) {
        Config config = new Config();

        String host = properties.getHost();
        int port = properties.getPort();
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
