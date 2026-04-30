package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentClosedEvent extends BaseDomainEvent {

    private static final String EVENT_TYPE = "PaymentClosed";

    private final Long paymentId;

    public PaymentClosedEvent(Long paymentId) {
        super(PaymentClosedEvent.class);
        this.paymentId = paymentId;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
