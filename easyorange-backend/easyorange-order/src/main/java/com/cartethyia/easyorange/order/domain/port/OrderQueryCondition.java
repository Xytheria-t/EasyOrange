package com.cartethyia.easyorange.order.domain.port;

import com.cartethyia.easyorange.order.domain.enums.OrderStatus;

/**
 * 订单查询条件 — 查询入口与仓储共用的唯一形状；status 为 {@link OrderStatus} 枚举（可 null 表示全部）。
 * <p>
 * 分页默认值与上限在这里兜底：调用方不该各自再写一遍，位置构造的字段顺序也只有一个定义。
 */
public record OrderQueryCondition(
        String orderNo, OrderStatus status, String buyerId, String sellerId, Integer pageNum, Integer pageSize) {

    public OrderQueryCondition {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
    }

    /** 把「我的订单 / 我售出的」的用户范围并进条件，保留其余过滤项。 */
    public OrderQueryCondition withParties(String buyerId, String sellerId) {
        return new OrderQueryCondition(orderNo, status, buyerId, sellerId, pageNum, pageSize);
    }
}
