package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的订单查询端口
 * 用于跨模块查询订单信息，遵循防腐层原则
 */
public interface AdminOrderQueryPort {

    /**
     * 查询订单列表（带条件查询）
     */
    OrderQueryResult queryOrders(OrderQueryCondition condition);

    /**
     * 根据订单 ID 列表批量查询订单项
     */
    Map<Long, List<OrderItemInfo>> getOrderItems(List<Long> orderIds);

    /**
     * 根据产品 ID 列表批量查询产品信息
     */
    Map<Long, ProductInfo> getProducts(List<Long> productIds);

    /**
     * 订单查询条件
     */
    record OrderQueryCondition(
        String orderNo,
        Long buyerId,
        Long sellerId,
        Integer status,
        Integer paymentStatus,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer pageNum,
        Integer pageSize
    ) {}

    /**
     * 订单查询结果
     */
    record OrderQueryResult(
        List<OrderSummary> records,
        long total,
        int pageNum,
        int pageSize
    ) {}

    /**
     * 订单摘要信息
     */
    record OrderSummary(
        Long id,
        String orderNo,
        Long buyerId,
        Long sellerId,
        BigDecimal totalAmount,
        Integer status,
        String statusDesc,
        Integer paymentStatus,
        String paymentStatusDesc,
        LocalDateTime createTime
    ) {}

    /**
     * 订单项信息
     */
    record OrderItemInfo(
        Long orderId,
        Long productId,
        Integer quantity,
        BigDecimal price
    ) {}

    /**
     * 产品信息（用于订单查询）
     */
    record ProductInfo(
        Long id,
        String name,
        BigDecimal price
    ) {}
}