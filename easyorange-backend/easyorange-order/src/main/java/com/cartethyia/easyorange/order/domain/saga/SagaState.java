package com.cartethyia.easyorange.order.domain.saga;

/**
 * Saga 状态机枚举。
 * <p>
 * 状态流转：
 * <pre>
 * PENDING → ORDER_CREATED → PAYMENT_CREATED → COMPLETED
 *    ↓           ↓              ↓
 *    └───────→ COMPENSATING → COMPENSATED
 *                   ↓
 *                FAILED
 *                   ↓ (超时未完成)
 *                TIMEOUT
 *                   ↓ (重试次数耗尽)
 *                MANUAL_INTERVENTION
 * </pre>
 * TIMEOUT：saga 在 PENDING/COMPENSATING 状态超过 30 分钟未更新
 * MANUAL_INTERVENTION：重试次数达到 {@link SagaStatus#MAX_RETRY_COUNT} 仍无法完成，需人工介入
 */
public enum SagaState {
    PENDING,
    ORDER_CREATED,
    PAYMENT_CREATED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED,
    /** 超时未完成 — saga 在活跃状态超过阈值未被推进 */
    TIMEOUT,
    /** 重试耗尽 — 需人工介入处理 */
    MANUAL_INTERVENTION
}
