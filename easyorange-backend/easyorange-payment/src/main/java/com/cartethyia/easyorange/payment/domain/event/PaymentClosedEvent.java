package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentClosedEvent(String eventId, String paymentId) implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}
