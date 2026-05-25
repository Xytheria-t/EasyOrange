package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCancelledEvent extends BaseDomainEvent {

    private final Long orderId;
    private final List<Long> productIds;
    private final String reason;

    public OrderCancelledEvent(Long orderId, List<Long> productIds, String reason) {
        super(OrderCancelledEvent.class);
        this.orderId = orderId;
        this.productIds = productIds;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return "OrderCancelled";
    }
}
