package com.cartethyia.easyorange.order.domain.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单读模型 — 查询侧数据载体。
 * <p>
 * status / paymentStatus 为 String code，与前端和 API 契约一致。
 */
public record OrderReadModel(
        String id,
        String orderNo,
        String buyerId,
        String sellerId,
        List<OrderItemReadModel> items,
        BigDecimal totalAmount,
        String status,
        String statusDesc,
        String paymentStatus,
        String address,
        String phone,
        String remark,
        String cancelReason,
        LocalDateTime cancelTime,
        String refundReason,
        LocalDateTime refundTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
