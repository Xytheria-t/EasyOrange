package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

public record OrderCancelledEvent(String orderId, List<String> productIds, String reason) implements OrderEvent {

    public OrderCancelledEvent {
        productIds = List.copyOf(productIds);
    }

    @Override
    public String orderId() {
        return orderId;
    }
}
