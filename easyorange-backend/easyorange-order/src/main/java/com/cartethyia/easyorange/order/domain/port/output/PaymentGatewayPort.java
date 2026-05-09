package com.cartethyia.easyorange.order.domain.port.output;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentGatewayPort extends OutboundPort {

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
