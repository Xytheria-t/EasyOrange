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

        String host = properties.getHost();
        int port = properties.getPort() > 0 ? properties.getPort() : 6379;
        String password = properties.getPassword();
        int database = properties.getDatabase();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(4);

        // 显式固定 watchdog 续期周期（显示默认 30s）：watchdog 按此周期为 leaseTime=-1 的锁续期，
        // 事务正常结束经 afterCompletion 释放即停。注意这**不是**持有上限——事务卡住时 watchdog 会一直续，
        // 上限靠 DistributedRedissonLockAdapter 的持有时长告警兜底（见 LockProperties.holdWarnThreshold）。
        config.setLockWatchdogTimeout(Duration.ofSeconds(30).toNanos());

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
