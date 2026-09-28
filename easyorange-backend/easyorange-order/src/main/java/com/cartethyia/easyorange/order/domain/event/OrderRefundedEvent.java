package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

/**
 * 订单退款事件 — 携带资产明细（ID + 数量），消费端据此按实际数量恢复库存并触发退款。
 * <p>
 * 明细不做 null 兜底，理由同 {@link OrderCancelledEvent}。
 */
public record OrderRefundedEvent(
        String eventId, String orderId, String buyerId, List<OrderItemRef> items, String reason) implements OrderEvent {

    public OrderRefundedEvent {
        items = List.copyOf(items);
    }
}
