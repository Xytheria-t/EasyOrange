package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentSucceededEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Payment";

    private final Long paymentId;
    private final String transactionId;

    public PaymentSucceededEvent(Long paymentId, String transactionId) {
        super(AGGREGATE_TYPE);
        this.paymentId = paymentId;
        this.transactionId = transactionId;
    }
}