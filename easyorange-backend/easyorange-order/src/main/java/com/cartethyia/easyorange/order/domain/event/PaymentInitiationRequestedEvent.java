package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentInitiationRequestedEvent extends BaseDomainEvent {

    private final String orderId;
    private final String buyerId;
    private final BigDecimal amount;
    private final Integer paymentMethod;
    private final String attach;
    private final String description;

    public PaymentInitiationRequestedEvent(
            String orderId,
            String buyerId,
            BigDecimal amount,
            Integer paymentMethod,
            String attach,
            String description
    ) {
        super();
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.attach = attach;
        this.description = description;
    }
}
