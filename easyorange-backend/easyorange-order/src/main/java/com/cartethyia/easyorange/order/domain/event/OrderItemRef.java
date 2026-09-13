package com.cartethyia.easyorange.order.domain.event;

import com.cartethyia.easyorange.common.util.BizRequire;

/**
 * 订单事件携带的资产明细 — 只带下游恢复 / 核对库存所需的最小信息（资产 ID + 数量）。
 * <p>
 * 数量必须随事件传递：库存恢复要按下单时扣减的数量回补，事件只带 productId 会迫使消费端猜 1，
 * 多件订单取消后库存就会永久少回。
 */
public record OrderItemRef(String productId, int quantity) {

    public OrderItemRef {
        BizRequire.notBlank(productId, "资产 ID 不能为空");
        BizRequire.requireTrue(quantity > 0, "数量必须大于 0");
    }
}
