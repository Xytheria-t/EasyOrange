package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAmount implements com.cartethyia.easyorange.common.ddd.ValueObject {

    private final BigDecimal value;

    public static PaymentAmount of(BigDecimal amount) {
        if (amount == null) {
            throw BusinessException.of("支付金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of("支付金额必须大于0");
        }
        return new PaymentAmount(amount);
    }

    public static PaymentAmount zero() {
        return new PaymentAmount(BigDecimal.ZERO);
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

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PaymentAmount that = (PaymentAmount) obj;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
