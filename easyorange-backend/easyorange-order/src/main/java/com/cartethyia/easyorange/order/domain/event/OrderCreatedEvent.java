package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class OrderCreatedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long buyerId;
    private final Long sellerId;
    private final List<OrderItemPayload> items;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Long orderId, Long buyerId, Long sellerId, List<OrderItemPayload> items, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @Getter
    public static class OrderItemPayload {
        private final Long productId;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        public OrderItemPayload(Long productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }
    }
}
