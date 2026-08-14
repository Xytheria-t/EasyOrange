package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

public record OrderRefundedEvent(String eventId, String orderId, String buyerId, List<String> productIds, String reason)
        implements OrderEvent {

    public OrderRefundedEvent {
        productIds = List.copyOf(productIds);
    }

    @Override
    public String orderId() {
        return orderId;
    }
}
