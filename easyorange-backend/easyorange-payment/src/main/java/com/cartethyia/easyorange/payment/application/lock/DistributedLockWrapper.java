package com.cartethyia.easyorange.payment.application.lock;

import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 分布式锁封装（基于 Redisson）。
 * <p>
 * 三个重载通过委托收敛到核心实现 {@link #executeWithLock(String, long, TimeUnit, Supplier)}，
 * 消除 try-finally + acquire/release 模板重复。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockWrapper {

    private final RedissonClient redissonClient;
    private final PaymentMetricsService metricsService;

    private static final String LOCK_PREFIX = "payment:lock:";
    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT, operation);
    }

    public void executeWithLock(String lockKey, Runnable operation) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        });
    }

    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation) {
        String fullKey = LOCK_PREFIX + lockKey;
        boolean locked = false;
        try {
            locked = acquireLock(fullKey, timeout, timeUnit);
            if (!locked) {
                log.warn("获取分布式锁失败: key={}", fullKey);
                metricsService.recordConcurrentConflict();
                throw new IllegalStateException("系统繁忙，请稍后重试");
            }
            return operation.get();
        } finally {
            if (locked) {
                releaseLock(fullKey);
            }
        }
    }

    private boolean acquireLock(String key, long timeout, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.tryLock(0, timeout, timeUnit);
        } catch (Exception e) {
            log.error("获取分布式锁异常: key={}", key, e);
            return false;
        }
    }

    private void releaseLock(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}", key, e);
        }
    }
}
