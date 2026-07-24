package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.math.BigDecimal;

public record PaymentCreatedEvent(
        String paymentId,
        String paymentNo,
        String orderId,
        String userId,
        BigDecimal amount,
        String paymentMethod
) implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}