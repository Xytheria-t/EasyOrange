package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.order.domain.saga.SagaLockAcquisitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁管理器
 * <p>
 * 负责分布式锁的获取和释放，提供锁的生命周期管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    private final RedisCache redisCache;

    /**
     * 执行带锁的操作
     *
     * @param lockKeys    锁的键列表
     * @param lockTimeout 锁的超时时间（秒）
     * @param operation   要执行的操作
     * @param <T>         操作返回类型
     * @return 操作结果
     * @throws SagaLockAcquisitionException 如果无法获取锁
     */
    public <T> T executeWithLocks(List<String> lockKeys, long lockTimeout, LockOperation<T> operation) {
        String lockValue = UUID.randomUUID().toString();
        List<String> acquiredKeys = new ArrayList<>();

        try {
            acquireLocks(lockKeys, lockValue, lockTimeout, acquiredKeys);
            return operation.execute();
        } finally {
            releaseLocks(acquiredKeys, lockValue);
        }
    }

    /**
     * 批量获取锁
     */
    private void acquireLocks(List<String> lockKeys, String lockValue, long timeout,
                               List<String> acquiredKeys) throws SagaLockAcquisitionException {
        for (String lockKey : lockKeys) {
            Boolean locked = redisCache.tryLock(lockKey, lockValue, timeout, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                releaseLocks(acquiredKeys, lockValue);
                throw new SagaLockAcquisitionException("商品下单繁忙，请稍后重试");
            }
            acquiredKeys.add(lockKey);
        }
    }

    /**
     * 批量释放锁（逆序释放，避免死锁）
     */
    private void releaseLocks(List<String> acquiredKeys, String lockValue) {
        for (int i = acquiredKeys.size() - 1; i >= 0; i--) {
            try {
                redisCache.unlock(acquiredKeys.get(i), lockValue);
            } catch (Exception e) {
                log.warn("释放锁失败 key={}", acquiredKeys.get(i), e);
            }
        }
    }

    /**
     * 锁操作接口
     */
    @FunctionalInterface
    public interface LockOperation<T> {
        T execute();
    }
}