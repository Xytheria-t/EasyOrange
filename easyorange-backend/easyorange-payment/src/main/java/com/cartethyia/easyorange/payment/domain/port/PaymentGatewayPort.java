package com.cartethyia.easyorange.payment.domain.port;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import java.math.BigDecimal;

public interface PaymentGatewayPort {

    PaymentResult pay(Payment aggregate);

    RefundResult refund(Payment aggregate, BigDecimal refundAmount);
}
