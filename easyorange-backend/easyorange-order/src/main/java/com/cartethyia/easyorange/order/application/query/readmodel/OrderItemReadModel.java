package com.cartethyia.easyorange.order.application.query.readmodel;

import com.cartethyia.easyorange.order.domain.valueobject.OrderItemSnapshot;
import java.math.BigDecimal;

/**
 * 订单项读模型 — 展示信息取自下单时的留痕快照（{@code eo_order_item.product_snapshot}），
 * 不读商品当前状态：资产被改名、改价或删除都不改变已下订单的展示。
 */
public record OrderItemReadModel(
        String itemId,
        String productId,
        OrderItemSnapshot snapshot,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal) {}
