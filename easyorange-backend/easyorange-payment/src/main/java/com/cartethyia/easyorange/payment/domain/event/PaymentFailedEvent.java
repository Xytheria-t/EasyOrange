package com.cartethyia.easyorange.payment.domain.event;

public record PaymentFailedEvent(String paymentId, String reason) implements PaymentConfirmEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}
