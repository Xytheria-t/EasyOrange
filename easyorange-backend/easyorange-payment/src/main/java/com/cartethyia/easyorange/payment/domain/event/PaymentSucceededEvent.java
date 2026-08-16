package com.cartethyia.easyorange.payment.domain.event;

public record PaymentSucceededEvent(String eventId, String paymentId, String orderId, String transactionId)
        implements PaymentConfirmEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}
