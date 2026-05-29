package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentGatewayPort {

    Long createPayment(CreatePaymentRequest request);

    void refundPayment(Long orderId, String reason);

    record CreatePaymentRequest(
            Long orderId,
            BigDecimal amount,
            Integer paymentMethod,
            String attach,
            String description
    ) {}
}