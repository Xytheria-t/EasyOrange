package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentFailedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Payment";
    private static final String EVENT_TYPE = "PaymentFailed";

    private final Long paymentId;
    private final String reason;

    public PaymentFailedEvent(Long paymentId, String reason) {
        super(AGGREGATE_TYPE);
        this.paymentId = paymentId;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}