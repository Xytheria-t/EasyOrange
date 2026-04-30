package com.cartethyia.easyorange.payment.domain.gateway;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult pay(PaymentAggregate aggregate);

    RefundResult refund(PaymentAggregate aggregate, BigDecimal refundAmount);
}
