package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentCreatedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Payment";
    private static final String EVENT_TYPE = "PaymentCreated";

    private final Long paymentId;
    private final String paymentNo;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final Integer paymentMethod;

    public PaymentCreatedEvent(Long paymentId, String paymentNo, Long orderId, Long userId,
                               BigDecimal amount, Integer paymentMethod) {
        super(AGGREGATE_TYPE);
        this.paymentId = paymentId;
        this.paymentNo = paymentNo;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}