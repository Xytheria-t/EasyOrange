package com.cartethyia.easyorange.order.domain.event;

import java.util.List;

/**
 * 订单取消事件 — 携带资产明细（ID + 数量），消费端据此按实际数量恢复库存。
 * <p>
 * 明细不做 null 兜底：缺行项属于载荷损坏（会产生事件的写操作必须加载行项），
 * 让它在构造点显性失败，好过让消费端对着空明细误报「载荷损坏」。
 */
public record OrderCancelledEvent(
        String eventId, String orderId, String buyerId, List<OrderItemRef> items, String reason) implements OrderEvent {

    public OrderCancelledEvent {
        items = List.copyOf(items);
    }
}
