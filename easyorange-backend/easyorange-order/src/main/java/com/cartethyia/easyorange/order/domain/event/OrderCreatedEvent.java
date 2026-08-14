package com.cartethyia.easyorange.order.domain.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String buyerId,
        String sellerId,
        List<OrderItemPayload> items,
        BigDecimal totalAmount)
        implements OrderEvent {

    public OrderCreatedEvent {
        items = List.copyOf(items);
    }

    @Override
    public String orderId() {
        return orderId;
    }

    public record OrderItemPayload(String productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
