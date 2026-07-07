package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.math.BigDecimal;

public record PaymentInitiationRequestedEvent(
        String orderId,
        String buyerId,
        BigDecimal amount,
        Integer paymentMethod,
        String attach,
        String description
) implements DomainEvent {
}
