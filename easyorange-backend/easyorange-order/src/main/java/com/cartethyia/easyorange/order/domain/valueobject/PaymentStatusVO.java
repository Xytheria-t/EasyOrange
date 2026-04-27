package com.cartethyia.easyorange.order.domain.valueobject;

public record PaymentStatusVO(Integer value) {
    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int REFUNDED = 2;

    public PaymentStatusVO {
        if (value == null) {
            throw new IllegalArgumentException("支付状态不能为空");
        }
    }

    public boolean isUnpaid() {
        return UNPAID == value;
    }

    public boolean isPaid() {
        return PAID == value;
    }

    public boolean isRefunded() {
        return REFUNDED == value;
    }
}