package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentRefundedEvent extends BaseDomainEvent {

    private final String paymentId;
    private final String refundReason;

    public PaymentRefundedEvent(String paymentId, String refundReason) {
        super();
        this.paymentId = paymentId;
        this.refundReason = refundReason;
    }
}