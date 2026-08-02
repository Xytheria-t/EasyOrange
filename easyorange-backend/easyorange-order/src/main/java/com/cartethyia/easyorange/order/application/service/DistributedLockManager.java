package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁管理器
 * <p>
 * 负责分布式锁的获取和释放，提供锁的生命周期管理。下单链路用它按 productId
 * 加锁，防止同一商品并发下单导致超卖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的操作
     *
     * @param lockKeys    锁的键列表
     * @param lockTimeout 锁的超时时间（秒）
     * @param operation   要执行的操作
     * @param <T>         操作返回类型
     * @return 操作结果
     * @throws OrderCreationException 如果无法获取锁
     */
    public <T> T executeWithLocks(List<String> lockKeys, long lockTimeout, LockOperation<T> operation) {
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            acquireLocks(lockKeys, lockTimeout, acquiredLocks);
            return operation.execute();
        } finally {
            releaseLocks(acquiredLocks);
        }
    }

    /**
     * 批量获取锁
     */
    private void acquireLocks(List<String> lockKeys, long timeout,
                                List<RLock> acquiredLocks) throws OrderCreationException {
        for (String lockKey : lockKeys) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean locked = lock.tryLock(timeout, timeout, TimeUnit.SECONDS);
                if (!locked) {
                    releaseLocks(acquiredLocks);
                    throw new OrderCreationException("资产下单繁忙，请稍后重试");
                }
                acquiredLocks.add(lock);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                releaseLocks(acquiredLocks);
                throw new OrderCreationException("资产下单繁忙，请稍后重试");
            }
        }
    }

    /**
     * 批量释放锁（逆序释放，避免死锁）
     */
    private void releaseLocks(List<RLock> acquiredLocks) {
        for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
            try {
                RLock lock = acquiredLocks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.warn("释放锁失败 key={}", acquiredLocks.get(i).getName(), e);
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
