package com.cartethyia.easyorange.payment.adapter.outbound.lock;

import com.cartethyia.easyorange.payment.domain.exception.LockAcquisitionException;
import com.cartethyia.easyorange.payment.domain.port.LockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁适配器 — {@link LockPort} 的 Redisson 实现。
 * <p>
 * 三个重载通过委托收敛到核心实现 {@link #executeWithLock(String, long, TimeUnit, Supplier)}，
 * 消除 try-finally + acquire/release 模板重复。
 */
@Slf4j
@Component("paymentRedissonLockAdapter")
@RequiredArgsConstructor
public class RedissonLockAdapter implements LockPort {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "payment:lock:";
    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT, operation);
    }

    @Override
    public void executeWithLock(String lockKey, Runnable operation) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation) {
        String fullKey = LOCK_PREFIX + lockKey;
        boolean locked = false;
        try {
            locked = acquireLock(fullKey, timeout, timeUnit);
            if (!locked) {
                log.warn("获取分布式锁失败: key={}", fullKey);
                throw new LockAcquisitionException("系统繁忙，请稍后重试");
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