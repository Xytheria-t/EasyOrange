package com.cartethyia.easyorange.framework.web.idempotency;

/**
 * Idempotency-Key 幂等服务。
 * <p>
 * 接收客户端提供的幂等 key，检查是否已处理过：
 * <ul>
 *   <li>已处理 → 返回缓存的成功响应</li>
 *   <li>未处理 → 执行业务操作并缓存结果</li>
 * </ul>
 * </p>
 * <p>
 * 执行期间抛出的异常不缓存，确保重试可重新执行。
 * </p>
 */
public interface IdempotencyService {

    /**
     * 幂等执行一个操作。
     *
     * @param key        客户端提供的幂等 key（如 Idempotency-Key 请求头）
     * @param ttlSeconds 缓存 TTL，过期后 key 自动失效
     * @param operation  待执行的业务操作
     * @param <T>        返回值类型
     * @return 操作结果（首次执行结果 或 已缓存的之前结果）
     * @throws Throwable 业务操作抛出的异常
     */
    <T> T execute(String key, long ttlSeconds, IdempotentOperation<T> operation) throws Throwable;

    /**
     * 检查 key 是否已处理（缓存中存在且未过期）。
     */
    boolean isProcessed(String key);

    /**
     * 主动失效一个幂等 key（如业务异常后的重试通道）。
     */
    void evict(String key);
}
