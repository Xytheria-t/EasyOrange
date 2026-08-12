package com.cartethyia.easyorange.framework.lock;

import java.util.List;

/**
 * 分布式锁端口 — 隔离具体锁实现（当前为 Redisson），订单/支付共用一份基础设施。
 * <p>
 * 统一语义（三处历史用法收敛后的唯一契约，调用方需遵守）：
 * <ul>
 *   <li><b>键排序</b>：多键由调用方自行排序后传入，适配器按序获取、逆序释放，避免死锁。</li>
 *   <li><b>等待/非阻塞</b>：{@code waitTimeoutSeconds} 是「获取锁的最长等待」；传 {@code 0} 即非阻塞尝试一次
 *       （拿不到立即失败），等同 tryLock 非阻塞语义。</li>
 *   <li><b>失败即抛</b>：获取锁失败（超时 / 中断）统一抛 {@link LockAcquisitionException}。
 *       需要「降级」的调用方在自身边界捕获该异常自行处理——锁不过 null 返回，避免静默吞下单。</li>
 *   <li><b>持有期</b>：租约交由 Redisson watchdog 续期（leaseTime=-1），覆盖整个操作；
 *       若操作运行在 {@code @Transactional} 内，释放推迟到事务提交/回滚之后，防止后一个请求读到未提交快照。</li>
 *   <li><b>同线程约束</b>：获取锁的线程必须把操作完整执行到结束。<b>禁止</b>在本端口内另开异步线程执行事务部分
 *       ——watchdog 续期与事务 {@code TransactionSynchronization} 均绑定当前线程，异步线程会击穿续期与延迟释放。</li>
 * </ul>
 */
public interface DistributedLockPort {

    /**
     * 执行带锁的操作（多键）。
     *
     * @param lockKeys            锁键列表，调用方需自行排序避免死锁；键前缀（命名空间）由调用方拼好，适配器不做加工
     * @param waitTimeoutSeconds  获取锁的最长等待秒数；{@code 0} = 非阻塞尝试一次
     * @param operation           锁内操作
     * @return 操作结果
     * @throws LockAcquisitionException 无法在 {@code waitTimeoutSeconds} 内获得全部锁，或获取过程被中断
     */
    <T> T executeWithLocks(List<String> lockKeys, long waitTimeoutSeconds, LockOperation<T> operation);

    /**
     * 单键便捷重载（无返回值）— 委托 {@link #executeWithLocks(List, long, LockOperation)}。
     */
    default void executeWithLock(String lockKey, long waitTimeoutSeconds, Runnable operation) {
        executeWithLocks(List.of(lockKey), waitTimeoutSeconds, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 锁内操作回调。
     */
    @FunctionalInterface
    interface LockOperation<T> {
        T execute();
    }
}