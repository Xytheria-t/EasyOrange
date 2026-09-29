package com.cartethyia.easyorange.framework.lock;

import java.util.List;

/**
 * 分布式锁端口 — 隔离具体锁实现（当前为 Redisson），订单/支付共用一份基础设施。
 * <p>
 * 统一语义：多键由调用方自行排序后传入，适配器按序获取——防 AB-BA 死锁靠的是统一加锁顺序，释放按获取逆序只是
 * 栈式回退惯例；{@code waitTimeoutSeconds} 是获取锁的最长等待，{@code 0} 即非阻塞尝试一次；获取失败（超时 / 中断）
 * 统一抛 {@link LockAcquisitionException}，需降级的调用方在自身边界捕获——锁不过 null 返回，避免静默吞下单。
 * <p>
 * 持有期由 Redisson watchdog 续期（leaseTime=-1）；操作在 {@code @Transactional} 内时释放推迟到事务提交/回滚之后，
 * 防止后一个请求读到未提交快照。
 * <p>
 * 获取锁的线程必须把操作跑完：禁止在锁内另开异步线程执行事务部分，续期与延迟释放都绑定当前线程。
 */
public interface DistributedLockPort {

    /**
     * 执行带锁的操作（多键）— 锁键由调用方排序，失败即抛 {@link LockAcquisitionException}。
     *
     * @throws LockAcquisitionException 无法在 {@code waitTimeoutSeconds} 内获得全部锁，或获取过程被中断
     */
    <T> T executeWithLocks(List<String> lockKeys, long waitTimeoutSeconds, LockOperation<T> operation);

    /** 单键便捷重载 — 委托 {@link #executeWithLocks(List, long, LockOperation)}。 */
    default void executeWithLock(String lockKey, long waitTimeoutSeconds, Runnable operation) {
        executeWithLocks(List.of(lockKey), waitTimeoutSeconds, () -> {
            operation.run();
            return null;
        });
    }

    @FunctionalInterface
    interface LockOperation<T> {
        T execute();
    }
}
