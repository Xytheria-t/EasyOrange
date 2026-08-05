package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;

/**
 * 支付状态守卫 —— 支付状态机的合法转换谓词。
 * <p>
 * 仅被 {@link Payment} 聚合根使用，判断某状态是否允许执行某动作
 * （支付/退款/关闭/失败/确认支付/确认退款）。与订单模块的
 * {@code OrderAction.canApply} 角色一致，只是订单把转换表收敛进枚举，
 * 支付把状态谓词收敛进本守卫类。
 */
public final class PaymentStatusGuard {

    private PaymentStatusGuard() {}

    public static boolean canPay(PaymentStatus status) {
        return PaymentStatus.PENDING.equals(status);
    }

    public static boolean canRefund(PaymentStatus status) {
        return PaymentStatus.SUCCESS.equals(status) || PaymentStatus.PARTIALLY_REFUNDED.equals(status);
    }

    public static boolean canClose(PaymentStatus status) {
        return PaymentStatus.PENDING.equals(status) || PaymentStatus.FAILED.equals(status);
    }

    public static boolean canFail(PaymentStatus status) {
        return PaymentStatus.PENDING.equals(status);
    }

    public static boolean canConfirmPay(PaymentStatus status) {
        return PaymentStatus.PAYING.equals(status);
    }

    public static boolean canConfirmRefund(PaymentStatus status) {
        return PaymentStatus.REFUNDING.equals(status);
    }
}
