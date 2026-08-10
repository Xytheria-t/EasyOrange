package com.cartethyia.easyorange.order.domain.constant;

/**
 * 订单关闭归因类型 — 决定关闭类动作的原因/时间落在哪一组字段上。
 * <p>
 * {@code NONE}：普通流转动作，无需原因；{@code CANCEL}：记入 cancel_reason/cancel_time；
 * {@code REFUND}：记入 refund_reason/refund_time。
 */
public enum ClosureKind {
    NONE,
    CANCEL,
    REFUND
}
