package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class OrderCreatedEvent extends BaseDomainEvent {

    private final String orderId;
    private final String buyerId;
    private final String sellerId;
    private final List<OrderItemPayload> items;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String buyerId, String sellerId, List<OrderItemPayload> items, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.items = List.copyOf(items);
        this.totalAmount = totalAmount;
    }

    public record OrderItemPayload(String productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
