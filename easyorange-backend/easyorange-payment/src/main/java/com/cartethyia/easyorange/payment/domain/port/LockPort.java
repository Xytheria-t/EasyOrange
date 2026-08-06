package com.cartethyia.easyorange.payment.domain.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁端口 — 隔离具体锁实现（当前为 Redisson）。
 * <p>
 * 支付/退款两阶段流程按业务单号加锁，防止同一支付单并发处理。
 * 应用层只依赖本端口，具体实现由 {@code RedissonLockAdapter} 提供。
 */
public interface LockPort {

    /**
     * 以默认超时执行带锁操作。
     */
    <T> T executeWithLock(String lockKey, Supplier<T> operation);

    /**
     * 以默认超时执行带锁操作（无返回值）。
     */
    void executeWithLock(String lockKey, Runnable operation);

    /**
     * 以指定超时执行带锁操作。
     */
    <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation);
}
