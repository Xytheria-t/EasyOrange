package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 订单已发货事件
 */
@Getter
public class OrderShippedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;

    public OrderShippedEvent(Long orderId) {
        super(AGGREGATE_TYPE);
        this.orderId = orderId;
    }
}
