package com.cartethyia.easyorange.framework.lock;

import com.cartethyia.easyorange.framework.config.properties.LockProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Redisson 分布式锁适配器 — {@link DistributedLockPort} 的统一实现，
 * 收敛了原 order / payment 模块各自的 Redisson 适配器（原 order 批量+watchdog+提交后释放，
 * original payment 固定租约+立即释放双份语义）。
 * <p>
 * 行为契约：
 * <ul>
 *   <li>按序获取调用方传入的多个锁键，任一把失败即逆序释放已获取的锁并抛 {@link LockAcquisitionException}。</li>
 *   <li>leaseTime={@code -1}：持有期由 Redisson watchdog 续期，避免固定租约在长事务结束前自动过期。</li>
 *   <li>锁的释放推迟到事务提交/回滚之后（{@link TransactionSynchronization#afterCompletion}）：
 *       若在事务方法体内提前释放，后一个请求会在前一个事务尚未提交时读到旧快照，击穿防超卖 /
 *       防并发重复处理；无事务（如定时任务）时立即释放。</li>
 * </ul>
 * ponytail: 持有期无硬性上限——watchdog 会持续续期，事务 hang 住时锁不会自动释放；
 * 唯一能设硬上限的做法是改回固定租约，但那会引入「租约早于事务结束过期」的并发窗口（四坑中
 * 完全相反的那一个），故保留 watchdog + 用 {@link LockProperties#getHoldWarnThreshold()} 监控长持有，
 * 异常时由人 / 运维介入。同一时刻持有锁的线程必须把事务完整跑完，禁止在锁内另开异步线程执行事务部分。
 */
@Slf4j
@Component("distributedLockAdapter")
@Primary
@EnableConfigurationProperties(LockProperties.class)
@RequiredArgsConstructor
public class DistributedRedissonLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;
    private final LockProperties lockProperties;

    @Override
    public <T> T executeWithLocks(List<String> lockKeys, long waitTimeoutSeconds, LockOperation<T> operation) {
        List<RLock> acquiredLocks = new ArrayList<>();
        long startNanos = System.nanoTime();
        try {
            acquireLocks(lockKeys, waitTimeoutSeconds, acquiredLocks);
            return operation.execute();
        } finally {
            releaseAfterCommit(acquiredLocks);
            long heldSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos);
            if (heldSeconds >= lockProperties.getHoldWarnThreshold().getSeconds()) {
                log.warn("分布式锁持有过久，疑似事务长时间卡住: keys={}, 持续={}s", lockKeys, heldSeconds);
            }
        }
    }

    /**
     * 批量获取锁 — 任一把失败即抛异常，由调用方 finally 逆序释放已获取锁。
     */
    private void acquireLocks(List<String> lockKeys, long timeout, List<RLock> acquiredLocks) {
        for (String lockKey : lockKeys) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                // leaseTime=-1：由 Redisson watchdog 续期，避免固定租约在操作结束前自动过期
                boolean locked = lock.tryLock(timeout, -1, TimeUnit.SECONDS);
                if (!locked) {
                    throw new LockAcquisitionException("无法在 " + timeout + " 秒内获取分布式锁: key=" + lockKey);
                }
                acquiredLocks.add(lock);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("获取分布式锁被中断: key=" + lockKey, e);
            }
        }
    }

    /**
     * 事务提交后再释放锁。若在事务内过早释放，后一个请求会在前一个事务尚未提交时读到旧快照。
     * 无事务（如定时任务、非事务编排）时立即释放。
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
     * 按获取逆序批量释放锁，确保资源依赖安全卸载。
     * 仅释放当前线程持有的锁，单锁释放异常不中断后续释放。
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
