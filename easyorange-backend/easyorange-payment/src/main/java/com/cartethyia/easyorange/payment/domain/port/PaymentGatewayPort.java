package com.cartethyia.easyorange.payment.domain.port;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    PaymentResult pay(PaymentAggregate aggregate);

    RefundResult refund(PaymentAggregate aggregate, BigDecimal refundAmount);
}