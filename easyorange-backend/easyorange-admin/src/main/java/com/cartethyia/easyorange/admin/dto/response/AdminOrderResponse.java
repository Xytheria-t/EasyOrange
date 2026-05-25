package com.cartethyia.easyorange.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
    Long orderId,
    String orderNo,
    Long buyerId,
    String buyerName,
    Long sellerId,
    String sellerName,
    List<ItemInfo> items,
    BigDecimal totalAmount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String paymentStatusDesc,
    LocalDateTime createTime
) {
    public record ItemInfo(Long productId, String productName) {}
}