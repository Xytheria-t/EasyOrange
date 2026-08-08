package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的订单查询/操作端口
 * 用于跨模块查询与操作订单信息，遵循防腐层原则
 */
public interface AdminOrderQueryPort {

    /**
     * 查询订单列表（带条件查询）
     */
    OrderQueryResult queryOrders(OrderQueryCondition condition);

    /**
     * 根据订单 ID 列表批量查询订单项
     */
    Map<String, List<OrderItemInfo>> getOrderItems(List<String> orderIds);

    /**
     * 根据产品 ID 列表批量查询产品信息
     */
    Map<String, ProductInfo> getProducts(List<String> productIds);

    /**
     * 根据订单 ID 查询订单详情，不存在或已删除时返回 null
     */
    OrderDetail getOrderDetail(String orderId);

    /**
     * 订单状态统计
     */
    OrderStats getOrderStats();

    /**
     * 取消订单（PENDING_PAYMENT 取消 / PAID 强制取消），不合法状态抛出 BusinessException
     */
    void cancelOrder(String orderId, String reason);

    /**
     * 强制完成订单（确认收货），不合法状态抛出 BusinessException
     */
    void forceComplete(String orderId);

    /**
     * 退款订单，不合法状态抛出 BusinessException
     */
    void refundOrder(String orderId, String reason);

    /**
     * 订单查询条件 — status/paymentStatus 为 String code（与 OrderStatus/PaymentStatus code 一致）。
     */
    record OrderQueryCondition(
            String orderNo,
            String buyerId,
            String sellerId,
            String status,
            String paymentStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    /**
     * 订单查询结果
     */
    record OrderQueryResult(List<OrderSummary> records, long total, int pageNum, int pageSize) {}

    /**
     * 订单摘要信息 — status/paymentStatus 为 String code。
     */
    record OrderSummary(
            String id,
            String orderNo,
            String buyerId,
            String sellerId,
            BigDecimal totalAmount,
            String status,
            String statusDesc,
            String paymentStatus,
            String paymentStatusDesc,
            LocalDateTime createTime) {}

    /**
     * 订单项信息
     */
    record OrderItemInfo(String orderId, String productId, Integer quantity, BigDecimal price) {}

    /**
     * 产品信息（用于订单查询）
     */
    record ProductInfo(String id, String name, BigDecimal price) {}

    /**
     * 订单详情
     */
    record OrderDetail(
            String id,
            String orderNo,
            String buyerId,
            String sellerId,
            List<OrderItemDetail> items,
            BigDecimal totalAmount,
            String status,
            String statusDesc,
            String paymentStatus,
            String remark,
            String cancelReason,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            LocalDateTime cancelTime) {}

    /**
     * 订单项详情
     */
    record OrderItemDetail(String productId, Integer quantity, BigDecimal price) {}

    /**
     * 订单状态统计
     */
    record OrderStats(
            long totalOrders,
            long todayOrders,
            long pendingPayment,
            long toShip,
            long toReceive,
            long completed,
            long cancelled,
            long refunded) {}
}
