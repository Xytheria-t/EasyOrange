package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 订单领域事件 —— sealed 接口，所有订单事件必须实现此接口。
 * <p>
 * 默认提供 {@link #aggregateId()} 实现，由 {@link #orderId()} 派生。
 */
public sealed interface OrderEvent extends DomainEvent
        permits OrderCreatedEvent, OrderPaidEvent, OrderShippedEvent,
                OrderCompletedEvent, OrderCancelledEvent, OrderRefundedEvent {

    /**
     * 订单 ID，所有订单事件都关联到一个订单。
     */
    String orderId();

    @Override
    default String aggregateId() {
        return orderId();
    }
}
