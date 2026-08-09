package com.cartethyia.easyorange.payment.domain.constant;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 支付状态机动作 — 支付状态合法转换的**唯一事实来源**。
 * <p>
 * 与订单的 {@code OrderAction}、商品的 {@code ProductAction} 角色一致，只是支付动作**无统一目标状态**：
 * 同一动作可能落到多个目标（确认支付 → SUCCESS/FAILED，确认退款 → REFUNDED/PARTIALLY_REFUNDED）且副作用字段各异，
 * 因此动作表只声明前置状态集合（sources），目标与副作用由各转换方法自行决定。聚合根经
 * {@code PaymentAction.X.canApply(status)} 裁决守卫。
 * <pre>
 * PENDING ──PAY──→ PAYING ──CONFIRM_PAY──→ SUCCESS / FAILED
 *   │                │  └──cancelPay──→ PENDING（回退，中间态）
 *   │                └──FAIL──→ FAILED ──CLOSE──→ CLOSED
 *   └──CLOSE──→ CLOSED
 * SUCCESS / PARTIALLY_REFUNDED ──REFUND──→ REFUNDING ──CONFIRM_REFUND──→ REFUNDED / PARTIALLY_REFUNDED
 * </pre>
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum PaymentAction {

    PAY("支付", Set.of(PaymentStatus.PENDING)),
    REFUND("退款", Set.of(PaymentStatus.SUCCESS, PaymentStatus.PARTIALLY_REFUNDED)),
    CLOSE("关闭", Set.of(PaymentStatus.PENDING, PaymentStatus.FAILED)),
    FAIL("失败", Set.of(PaymentStatus.PENDING)),
    CONFIRM_PAY("确认支付", Set.of(PaymentStatus.PAYING)),
    CONFIRM_REFUND("确认退款", Set.of(PaymentStatus.REFUNDING));

    /** 动作名称（用于日志/提示） */
    private final String actionName;
    /** 允许触发该动作的前置状态集合 */
    private final Set<PaymentStatus> sources;

    /**
     * 当前状态是否允许触发该动作。
     */
    public boolean canApply(PaymentStatus currentStatus) {
        return sources.contains(currentStatus);
    }
}
