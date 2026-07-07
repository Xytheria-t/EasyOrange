package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentRefundedEvent(String paymentId, String refundReason) implements DomainEvent {
}