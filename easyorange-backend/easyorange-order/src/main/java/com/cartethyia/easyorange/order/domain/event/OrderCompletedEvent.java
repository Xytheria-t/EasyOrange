package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class OrderCompletedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long productId;

    public OrderCompletedEvent(Long orderId, Long productId) {
        super(OrderCompletedEvent.class);
        this.orderId = orderId;
        this.productId = productId;
    }

    @Override
    public String eventType() {
        return "OrderCompleted";
    }
}
