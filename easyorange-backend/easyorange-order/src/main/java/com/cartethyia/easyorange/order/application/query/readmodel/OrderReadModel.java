package com.cartethyia.easyorange.order.application.query.readmodel;

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
        LocalDateTime updateTime) {

    /**
     * 是否为订单参与方（买家或卖家）— 查询侧的所有权规则收口于此。
     * <p>
     * 读模型是 CQRS 查询侧的独立数据载体（{@code buyerId}/{@code sellerId} 为 String，与 API 契约一致），
     * 与命令侧聚合根的角色谓词（{@code Order.isBuyer/isSeller}）各自表达，但错误码同为 {@code ORDER_NOT_OWNER}。
     */
    public boolean isOwnedBy(String userId) {
        return userId.equals(buyerId) || userId.equals(sellerId);
    }
}
