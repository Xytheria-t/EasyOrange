package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.order.domain.constant.OrderStatus;

/**
 * 订单列表查询参数对象 — 收敛 listOrders 的 6 个参数为单一 record。
 * <p>
 * status 为 {@link OrderStatus} 枚举（可 null 表示全部），由 Controller 层将
 * 前端 String code 转换为枚举，类型安全下沉到 application/domain 层。
 */
public record OrderListQuery(
        String orderNo, OrderStatus status, String buyerId, String sellerId, Integer pageNum, Integer pageSize) {
    public OrderListQuery {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
    }
}
