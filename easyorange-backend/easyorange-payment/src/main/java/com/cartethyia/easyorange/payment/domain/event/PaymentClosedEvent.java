package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentClosedEvent extends BaseDomainEvent {

    private final String paymentId;

    public PaymentClosedEvent(String paymentId) {
        super();
        this.paymentId = paymentId;
    }
}
