package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record PaymentClosedEvent(String paymentId) implements DomainEvent {
}
