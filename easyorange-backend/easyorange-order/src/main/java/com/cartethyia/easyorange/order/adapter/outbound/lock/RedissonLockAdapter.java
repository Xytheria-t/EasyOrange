package com.cartethyia.easyorange.order.adapter.outbound.lock;

import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
import com.cartethyia.easyorange.order.domain.port.LockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁适配器 — {@link LockPort} 的 Redisson 实现。
 * <p>
 * 负责分布式锁的获取和释放，提供锁的生命周期管理。下单链路按 productId 加锁，
 * 防止同一商品并发下单导致超卖。
 */
@Slf4j
@Component("orderRedissonLockAdapter")
@RequiredArgsConstructor
public class RedissonLockAdapter implements LockPort {

    private final RedissonClient redissonClient;

    @Override
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
     * 批量获取锁。
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
     * 批量释放锁（逆序释放，避免死锁）。
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
}