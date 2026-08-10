package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AdminOrderDetailResponse(
        String orderId,
        String orderNo,
        BuyerInfo buyer,
        SellerInfo seller,
        List<ProductInfo> products,
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
    public record BuyerInfo(String userId, String nickname, String avatar, String phone) {}

    public record SellerInfo(String userId, String nickname, String avatar, String phone) {}

    public record ProductInfo(String productId, String name, String mainImage, BigDecimal price) {}

    public record Address(String receiverName, String phone, String detailAddress) {}
}
