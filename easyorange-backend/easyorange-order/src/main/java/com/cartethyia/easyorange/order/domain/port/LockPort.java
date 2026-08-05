package com.cartethyia.easyorange.order.domain.port;

import java.util.List;

/**
 * 分布式锁端口 — 隔离具体锁实现（当前为 Redisson）。
 * <p>
 * 下单链路按 productId 加锁防止同一商品并发下单导致超卖。
 * 应用层只依赖本端口，具体实现由 {@code RedissonLockAdapter} 提供。
 */
public interface LockPort {

    /**
     * 执行带锁的操作。
     *
     * @param lockKeys    锁的键列表（调用方需自行排序以避免死锁）
     * @param lockTimeout 锁的超时时间（秒）
     * @param operation   要执行的操作
     * @param <T>         操作返回类型
     * @return 操作结果
     * @throws com.cartethyia.easyorange.order.domain.exception.OrderCreationException 无法获取锁时
     */
    <T> T executeWithLocks(List<String> lockKeys, long lockTimeout, LockOperation<T> operation);

    /**
     * 锁操作回调。
     */
    @FunctionalInterface
    interface LockOperation<T> {
        T execute();
    }
}