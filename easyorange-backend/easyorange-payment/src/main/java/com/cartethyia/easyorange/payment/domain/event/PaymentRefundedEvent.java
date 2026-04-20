package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentRefundedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Payment";

    private final Long paymentId;
    private final String refundReason;

    public PaymentRefundedEvent(Long paymentId, String refundReason) {
        super(AGGREGATE_TYPE);
        this.paymentId = paymentId;
        this.refundReason = refundReason;
    }
}