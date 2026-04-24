package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 订单创建事件
 */
@Getter
public class OrderCreatedEvent extends BaseDomainEvent {

    private final Long orderId;
    private final Long buyerId;
    private final Long sellerId;
    private final Long productId;
    private final BigDecimal amount;

    public OrderCreatedEvent(Long orderId, Long buyerId, Long sellerId, Long productId, BigDecimal amount) {
        super(OrderCreatedEvent.class);
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
    }

    public Long getOrderId() { return orderId; }
    public Long getBuyerId() { return buyerId; }
    public Long getSellerId() { return sellerId; }
    public Long getProductId() { return productId; }
    public BigDecimal getAmount() { return amount; }

    @Override
    public String eventType() {
        return "OrderCreated";
    }
}
