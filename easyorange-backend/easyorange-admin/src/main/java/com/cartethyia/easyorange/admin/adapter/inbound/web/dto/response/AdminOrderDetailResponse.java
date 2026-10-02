package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AdminOrderDetailResponse(
        String orderId,
        String orderNo,
        Participant buyer,
        Participant seller,
        List<ItemInfo> items,
        BigDecimal totalAmount,
        String status,
        String statusDesc,
        String paymentStatus,
        String paymentNo,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        Address shippingAddress,
        String remark,
        String cancelReason,
        LocalDateTime createTime,
        LocalDateTime payTime,
        LocalDateTime updateTime,
        LocalDateTime cancelTime,
        String refundReason,
        LocalDateTime refundTime) {

    /** 买卖双方档案同构，共用一个 record —— 前端也只声明一个 {@code OrderParticipant}。 */
    public record Participant(String userId, String nickname, String avatar, String phone) {}

    /**
     * 订单行项 —— 字段名与前端 {@code AdminOrderDetailItem} 对齐。
     *
     * <p>{@code subtotal} 在服务端算好：前端只做展示，让它自己乘一遍等于多一处可能对不上的真相。
     * {@code productImage} 可为 null（商品主图在 {@code eo_product_image} 另一张表，
     * 行项快照不带图），前端有占位图兜底。
     */
    public record ItemInfo(
            String itemId,
            String productId,
            String productName,
            String productImage,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal) {}

    public record Address(String receiverName, String phone, String detailAddress) {}
}
