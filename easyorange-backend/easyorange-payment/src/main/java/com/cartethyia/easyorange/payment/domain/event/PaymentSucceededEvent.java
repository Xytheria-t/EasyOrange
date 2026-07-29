package com.cartethyia.easyorange.payment.domain.event;

public record PaymentSucceededEvent(String paymentId, String transactionId) implements PaymentConfirmEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}