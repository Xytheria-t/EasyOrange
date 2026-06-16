package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentFailedEvent extends BaseDomainEvent {

    private final Long paymentId;
    private final String reason;

    public PaymentFailedEvent(Long paymentId, String reason) {
        super();
        this.paymentId = paymentId;
        this.reason = reason;
    }
}