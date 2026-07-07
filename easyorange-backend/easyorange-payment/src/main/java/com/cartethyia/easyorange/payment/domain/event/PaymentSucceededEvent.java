package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentSucceededEvent(String paymentId, String transactionId) implements DomainEvent {
}