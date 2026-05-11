package com.cartethyia.easyorange.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderVO(
    Long orderId,
    String orderNo,
    Long buyerId,
    String buyerName,
    Long sellerId,
    String sellerName,
    Long productId,
    String productName,
    BigDecimal amount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String paymentStatusDesc,
    LocalDateTime createTime
) {}
