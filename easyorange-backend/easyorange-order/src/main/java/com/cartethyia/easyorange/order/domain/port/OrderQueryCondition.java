package com.cartethyia.easyorange.order.domain.port;

import com.cartethyia.easyorange.order.domain.constant.OrderStatus;

/**
 * 订单查询条件 — status 为 {@link OrderStatus} 枚举（可 null 表示全部）。
 */
public record OrderQueryCondition(
        String orderNo, OrderStatus status, String buyerId, String sellerId, Integer pageNum, Integer pageSize) {}
