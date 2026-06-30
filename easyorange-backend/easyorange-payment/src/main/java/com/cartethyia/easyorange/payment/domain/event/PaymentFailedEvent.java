package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentFailedEvent extends BaseDomainEvent {

    private final String paymentId;
    private final String reason;

    public PaymentFailedEvent(String paymentId, String reason) {
        super();
        this.paymentId = paymentId;
        this.reason = reason;
    }
}