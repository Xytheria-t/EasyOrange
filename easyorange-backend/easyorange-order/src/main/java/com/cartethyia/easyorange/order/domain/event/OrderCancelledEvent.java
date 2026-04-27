package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class OrderCancelledEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long productId;
    private final String reason;

    public OrderCancelledEvent(Long orderId, Long productId, String reason) {
        super(OrderCancelledEvent.class);
        this.orderId = orderId;
        this.productId = productId;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return "OrderCancelled";
    }
}
