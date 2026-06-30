package com.cartethyia.easyorange.order.domain.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderReadModel(
    String id,
    String orderNo,
    String buyerId,
    String sellerId,
    List<OrderItemReadModel> items,
    BigDecimal totalAmount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String address,
    String phone,
    String remark,
    String cancelReason,
    LocalDateTime cancelTime,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
