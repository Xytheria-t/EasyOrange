package com.cartethyia.easyorange.payment.domain.factory;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.math.BigDecimal;

public class PaymentFactory {

    private PaymentFactory() {}

    public static PaymentAggregate create(Long orderId, Long userId, BigDecimal amount,
                                          Integer paymentMethodCode, String attach) {
        return PaymentAggregate.create(orderId, userId, amount, paymentMethodCode, attach);
    }
}
