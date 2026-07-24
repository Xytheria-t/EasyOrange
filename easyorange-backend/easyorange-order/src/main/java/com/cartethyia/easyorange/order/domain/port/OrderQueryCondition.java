package com.cartethyia.easyorange.order.domain.port;

/**
 * 订单查询条件 — status 为 String code（与 OrderStatus.code 一致）。
 */
public record OrderQueryCondition(
        String orderNo,
        String status,
        String buyerId,
        String sellerId,
        Integer pageNum,
        Integer pageSize
) {}
