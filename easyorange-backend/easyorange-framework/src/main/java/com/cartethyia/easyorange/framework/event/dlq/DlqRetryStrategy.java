package com.cartethyia.easyorange.framework.event.dlq;

/**
 * DLQ 重试策略 — 控制死信消息的重试次数与退避延迟。
 * <p>
 * 三级重试架构：
 * <ol>
 *   <li>主队列：RetryTemplate 指数退避 3 次（由 {@code domainEventContainerFactory} 配置）</li>
 *   <li>DLQ：{@link DlqRetryScheduler} 定时拉取，按本策略决定重投或转储</li>
 *   <li>Terminal 队列：超过 {@link #getMaxRetries()} 的毒消息转储，等待人工介入</li>
 * </ol>
 */
public interface DlqRetryStrategy {

    /**
     * 是否应重试。
     *
     * @param retryCount 当前重试次数（0 = 首次进入 DLQ，1 = 已重投 1 次后再次进入 DLQ）
     * @return true 表示应重投主队列，false 表示转储 terminal 队列
     */
    boolean shouldRetry(int retryCount);

    /**
     * 获取重试前的退避延迟（毫秒）。
     * <p>
     * 实际延迟由 {@link DlqRetryScheduler} 的调度间隔决定，此值用于日志记录与未来 TTL 延迟重投扩展。
     *
     * @param retryCount 当前重试次数
     * @return 退避毫秒数
     */
    long getDelayMillis(int retryCount);

    /**
     * 最大重试次数。超过此值的消息转储 terminal 队列。
     */
    int getMaxRetries();
}
