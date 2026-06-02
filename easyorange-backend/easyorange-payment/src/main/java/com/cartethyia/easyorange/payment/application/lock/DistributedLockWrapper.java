package com.cartethyia.easyorange.payment.application.lock;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockWrapper {

    private final RedisCache redisCache;
    
    private static final String LOCK_PREFIX = "payment:lock:";
    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        String lockValue = UUID.randomUUID().toString();
        String fullKey = LOCK_PREFIX + lockKey;
        
        boolean locked = false;
        try {
            locked = acquireLock(fullKey, lockValue);
            if (!locked) {
                log.warn("获取分布式锁失败: key={}", fullKey);
                throw new IllegalStateException("系统繁忙，请稍后重试");
            }
            
            return operation.get();
        } finally {
            if (locked) {
                releaseLock(fullKey, lockValue);
            }
        }
    }

    public void executeWithLock(String lockKey, Runnable operation) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        });
    }

    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation) {
        String lockValue = UUID.randomUUID().toString();
        String fullKey = LOCK_PREFIX + lockKey;
        
        boolean locked = false;
        try {
            locked = acquireLock(fullKey, lockValue, timeout, timeUnit);
            if (!locked) {
                log.warn("获取分布式锁失败: key={}, timeout={} {}", fullKey, timeout, timeUnit);
                throw new IllegalStateException("系统繁忙，请稍后重试");
            }
            
            return operation.get();
        } finally {
            if (locked) {
                releaseLock(fullKey, lockValue);
            }
        }
    }

    private boolean acquireLock(String key, String value) {
        return acquireLock(key, value, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    private boolean acquireLock(String key, String value, long timeout, TimeUnit timeUnit) {
        try {
            Boolean result = redisCache.tryLock(key, value, timeout, timeUnit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁异常: key={}", key, e);
            return false;
        }
    }

    private void releaseLock(String key, String value) {
        try {
            Boolean result = redisCache.unlock(key, value);
            if (!Boolean.TRUE.equals(result)) {
                log.warn("释放分布式锁失败: key={}", key);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}", key, e);
        }
    }
}
