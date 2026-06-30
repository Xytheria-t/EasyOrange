package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentCreatedEvent extends BaseDomainEvent {

    private final String paymentId;
    private final String paymentNo;
    private final String orderId;
    private final String userId;
    private final BigDecimal amount;
    private final Integer paymentMethod;

    public PaymentCreatedEvent(String paymentId, String paymentNo, String orderId, String userId,
                               BigDecimal amount, Integer paymentMethod) {
        super();
        this.paymentId = paymentId;
        this.paymentNo = paymentNo;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
}