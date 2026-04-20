package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 订单创建事件
 */
@Getter
public class OrderCreatedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Long buyerId;
    private final Long sellerId;
    private final Long productId;
    private final BigDecimal amount;

    public OrderCreatedEvent(Long orderId, Long buyerId, Long sellerId, Long productId, BigDecimal amount) {
        super(AGGREGATE_TYPE);
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
    }
}
