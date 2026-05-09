package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentInitiationRequestedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long buyerId;
    private final BigDecimal amount;
    private final Integer paymentMethod;
    private final String attach;
    private final String description;

    public PaymentInitiationRequestedEvent(
            Long orderId,
            Long buyerId,
            BigDecimal amount,
            Integer paymentMethod,
            String attach,
            String description
    ) {
        super(PaymentInitiationRequestedEvent.class);
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.attach = attach;
        this.description = description;
    }

    @Override
    public String eventType() {
        return "PaymentInitiationRequested";
    }
}
