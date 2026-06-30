package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PaymentSucceededEvent extends BaseDomainEvent {

    private final String paymentId;
    private final String transactionId;

    public PaymentSucceededEvent(String paymentId, String transactionId) {
        super();
        this.paymentId = paymentId;
        this.transactionId = transactionId;
    }
}