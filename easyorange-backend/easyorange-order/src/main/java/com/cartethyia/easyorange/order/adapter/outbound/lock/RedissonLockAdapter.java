package com.cartethyia.easyorange.order.adapter.outbound.lock;

import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
import com.cartethyia.easyorange.order.domain.port.LockPort;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Redisson 分布式锁适配器 — {@link LockPort} 的 Redisson 实现。
 * <p>
 * 负责分布式锁的获取和释放，提供锁的生命周期管理。下单链路按 productId 加锁，
 * 防止同一商品并发下单导致超卖（排队串行）。
 * <p>
 * 锁的释放推迟到事务提交/回滚之后（{@link TransactionSynchronization#afterCompletion}）：
 * 若在 {@code @Transactional} 方法体内提前释放，后一个请求会在前一个事务尚未提交时
 * 读到旧库存快照，击穿防超卖。leaseTime 传 {@code -1}，由 Redisson watchdog 续期，
 * 锁不会在下单流程结束前自动过期。
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
            releaseAfterCommit(acquiredLocks);
        }
    }

    /**
     * 批量获取锁。
     */
    private void acquireLocks(List<String> lockKeys, long timeout, List<RLock> acquiredLocks)
            throws OrderCreationException {
        for (String lockKey : lockKeys) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                // leaseTime=-1：由 Redisson watchdog 续期，避免固定租约在下单流程结束前自动过期
                boolean locked = lock.tryLock(timeout, -1, TimeUnit.SECONDS);
                if (!locked) {
                    throw new OrderCreationException("资产下单繁忙，请稍后重试");
                }
                acquiredLocks.add(lock);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OrderCreationException("资产下单繁忙，请稍后重试");
            }
        }
    }

    /**
     * 事务提交后再释放锁。若在事务内（下单链路）过早释放，
     * 后一个请求会在前一个事务尚未提交时读到旧库存快照，击穿防超卖。
     */
    private void releaseAfterCommit(List<RLock> acquiredLocks) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(acquiredLocks);
                }
            });
        } else {
            releaseLocks(acquiredLocks);
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
