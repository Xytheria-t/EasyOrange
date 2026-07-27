package com.cartethyia.easyorange.framework.event.dlq;

import org.springframework.stereotype.Component;

/**
 * 指数退避 DLQ 重试策略 — 最大 3 次重试，退避梯度 1min → 5min → 15min。
 * <p>
 * 重试梯度设计：
 * <ul>
 *   <li>第 1 次重试（retryCount=0）：等待 1 分钟</li>
 *   <li>第 2 次重试（retryCount=1）：等待 5 分钟</li>
 *   <li>第 3 次重试（retryCount=2）：等待 15 分钟</li>
 *   <li>超过 3 次（retryCount≥3）：转储 terminal 队列</li>
 * </ul>
 * 退避值为日志参考值；实际延迟由 {@link DlqRetryScheduler} 调度间隔（默认 5 分钟）决定。
 */
@Component
public class ExponentialBackoffRetryStrategy implements DlqRetryStrategy {

    private static final int MAX_RETRIES = 3;
    private static final long[] DELAY_MILLIS = {60_000L, 300_000L, 900_000L};

    @Override
    public boolean shouldRetry(int retryCount) {
        return retryCount < MAX_RETRIES;
    }

    @Override
    public long getDelayMillis(int retryCount) {
        if (retryCount < 0) return DELAY_MILLIS[0];
        if (retryCount >= DELAY_MILLIS.length) return DELAY_MILLIS[DELAY_MILLIS.length - 1];
        return DELAY_MILLIS[retryCount];
    }

    @Override
    public int getMaxRetries() {
        return MAX_RETRIES;
    }
}
