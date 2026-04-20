package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Objects;

public final class PaymentStatusVO implements ValueObject {

    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int REFUNDED = 2;

    private final Integer value;

    public PaymentStatusVO(Integer value) {
        this.value = Objects.requireNonNull(value, "支付状态不能为空");
    }

    public Integer value() {
        return value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentStatusVO that = (PaymentStatusVO) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PaymentStatusVO{" + value + '}';
    }
}
