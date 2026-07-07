package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 订单已发货事件
 */
public record OrderShippedEvent(String orderId) implements DomainEvent {
}
