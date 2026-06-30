package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
    String orderId,
    String orderNo,
    String buyerId,
    String buyerName,
    String sellerId,
    String sellerName,
    List<ItemInfo> items,
    BigDecimal totalAmount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String paymentStatusDesc,
    LocalDateTime createTime
) {
    public record ItemInfo(String productId, String productName) {}
}