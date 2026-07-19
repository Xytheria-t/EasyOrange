package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentFailedEvent(String paymentId, String reason) implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}