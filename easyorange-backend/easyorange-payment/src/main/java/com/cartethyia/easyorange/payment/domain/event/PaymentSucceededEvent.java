package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentSucceededEvent(String eventId, String paymentId, String orderId, String transactionId)
        implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}
