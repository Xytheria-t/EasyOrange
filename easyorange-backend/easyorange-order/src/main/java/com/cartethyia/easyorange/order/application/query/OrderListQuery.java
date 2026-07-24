package com.cartethyia.easyorange.order.application.query;

/**
 * 订单列表查询参数对象 — 收敛 listOrders 的 6 个参数为单一 record。
 * <p>
 * status 为 String code（与 {@code OrderStatus.code} 一致）。
 */
public record OrderListQuery(
        String orderNo,
        String status,
        String buyerId,
        String sellerId,
        Integer pageNum,
        Integer pageSize
) {
    public OrderListQuery {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
    }
}
