package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentRefundedEvent extends BaseDomainEvent {

    private static final String EVENT_TYPE = "PaymentRefunded";

    private final Long paymentId;
    private final String refundReason;

    public PaymentRefundedEvent(Long paymentId, String refundReason) {
        super(PaymentRefundedEvent.class);
        this.paymentId = paymentId;
        this.refundReason = refundReason;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}