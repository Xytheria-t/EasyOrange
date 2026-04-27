package com.cartethyia.easyorange.payment.domain.strategy;

import java.math.BigDecimal;

public interface PaymentStrategy {

    PaymentResult pay(Long paymentId, Long orderId, BigDecimal amount, Integer paymentMethod);

    RefundResult refund(Long paymentId, BigDecimal refundAmount);
}
