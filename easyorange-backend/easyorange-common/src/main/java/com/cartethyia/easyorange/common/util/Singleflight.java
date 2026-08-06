package com.cartethyia.easyorange.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 本地 Singleflight：同一 key 的并发请求只执行一次 supplier，其他请求等待结果复用。
 * <p>
 * 用于缓存击穿防护（cache stampede protection）——高并发下同一 cache key 的 miss 只回源一次。
 *
 * <p>线程安全：基于 {@link ConcurrentHashMap} + {@code putIfAbsent} 实现 race-safe 注册。
 * <br>内存安全：{@code finally} 移除 entry，防止 map 无限增长。
 * <br>异常传播：通过 {@link #joinUnwrapping} 还原原始异常（如 {@code ProductNotFoundException}）。
 */
public final class Singleflight<K, V> {

    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    /**
     * 对同一 key 的并发调用只执行一次 supplier。
     * <p>
     * 赢得 {@code putIfAbsent} 竞争的线程（leader）在自身线程同步执行 supplier 并完成 future；
     * 其他线程（follower）通过 {@code get} 或 {@code putIfAbsent} 拿到 leader 的 future 后 join 等待。
     *
     * @param key      去重 key
     * @param supplier 实际执行逻辑（只对 leader 执行）
     * @return supplier 的返回值（所有调用者拿到相同结果）
     */
    public V execute(K key, Supplier<V> supplier) {
        var existing = inFlight.get(key);
        if (existing != null) {
            return joinUnwrapping(existing);
        }
        var newFuture = new CompletableFuture<V>();
        var winner = inFlight.putIfAbsent(key, newFuture);
        if (winner != null) {
            return joinUnwrapping(winner);
        }
        try {
            V result = supplier.get();
            newFuture.complete(result);
            return result;
        } catch (RuntimeException | Error ex) {
            newFuture.completeExceptionally(ex);
            throw ex;
        } finally {
            inFlight.remove(key, newFuture);
        }
    }

    /**
     * Join future 并解包 {@link CompletionException}，还原原始 RuntimeException/Error。
     */
    private static <V> V joinUnwrapping(CompletableFuture<V> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            var cause = ex.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error e) throw e;
            throw new RuntimeException(cause);
        }
    }
}
