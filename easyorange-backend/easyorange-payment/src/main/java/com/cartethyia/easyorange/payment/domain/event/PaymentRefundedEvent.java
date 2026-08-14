package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentRefundedEvent(String eventId, String paymentId, String refundReason) implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}
