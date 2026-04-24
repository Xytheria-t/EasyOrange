package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;

import java.math.BigDecimal;

public record PaymentAmount(BigDecimal value) {
    public PaymentAmount {
        if (value == null) {
            throw BusinessException.of("支付金额不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of("支付金额必须大于0");
        }
    }

    public static PaymentAmount of(BigDecimal amount) {
        return new PaymentAmount(amount);
    }

    public boolean isGreaterThan(BigDecimal other) {
        return value.compareTo(other) > 0;
    }

    public boolean isLessThanOrEqualTo(BigDecimal other) {
        return value.compareTo(other) <= 0;
    }

    public boolean isEqualTo(BigDecimal other) {
        return value.compareTo(other) == 0;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}