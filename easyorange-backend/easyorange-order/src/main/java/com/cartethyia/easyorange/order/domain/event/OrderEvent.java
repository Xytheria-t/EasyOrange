package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;

/**
 * 订单领域事件 —— sealed 接口，所有订单事件必须实现此接口。
 * <p>
 * 默认提供 {@link #aggregateId()} 与 {@link #orderNo()} 派生实现，均由 {@link #orderId()} 推出，
 * 事件载荷不必重复携带可推导的值。
 */
public sealed interface OrderEvent extends DomainEvent
        permits OrderCreatedEvent,
                OrderPaidEvent,
                OrderShippedEvent,
                OrderCompletedEvent,
                OrderCancelledEvent,
                OrderRefundedEvent {

    /**
     * 订单 ID，所有订单事件都关联到一个订单。
     */
    String orderId();

    /**
     * 买家 ID — 所有订单事件携带买家，下游通知/信用重算自包含消费，无需回查订单。
     */
    String buyerId();

    /**
     * 订单号 — 由 {@link #orderId()} 派生（规则收口在 {@link OrderNo#forOrderId}），
     * 下游展示场景直接用它，无需回查订单或自行拼前缀。
     */
    default OrderNo orderNo() {
        return OrderNo.forOrderId(orderId());
    }

    @Override
    default String aggregateId() {
        return orderId();
    }
}
