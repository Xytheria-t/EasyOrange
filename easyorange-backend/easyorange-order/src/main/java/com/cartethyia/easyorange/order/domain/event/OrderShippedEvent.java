package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 订单已发货事件
 */
@Getter
public class OrderShippedEvent extends BaseDomainEvent {

    private final String orderId;

    public OrderShippedEvent(String orderId) {
        super();
        this.orderId = orderId;
    }
}
