package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentSucceededEvent extends BaseDomainEvent {

    private static final String EVENT_TYPE = "PaymentSucceeded";

    private final Long paymentId;
    private final String transactionId;

    public PaymentSucceededEvent(Long paymentId, String transactionId) {
        super(PaymentSucceededEvent.class);
        this.paymentId = paymentId;
        this.transactionId = transactionId;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}