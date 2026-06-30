package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentGatewayPort {

    String createPayment(CreatePaymentRequest request);

    void refundPayment(String orderId, String reason);

    record CreatePaymentRequest(
            String orderId,
            BigDecimal amount,
            Integer paymentMethod,
            String attach,
            String description
    ) {}
}