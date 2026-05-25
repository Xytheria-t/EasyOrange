package com.cartethyia.easyorange.order.domain.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderReadModel(
    Long id,
    String orderNo,
    Long buyerId,
    Long sellerId,
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
