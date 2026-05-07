package com.cartethyia.easyorange.payment.domain.specification;

import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;

public final class PaymentSpecification {

    private PaymentSpecification() {}

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
