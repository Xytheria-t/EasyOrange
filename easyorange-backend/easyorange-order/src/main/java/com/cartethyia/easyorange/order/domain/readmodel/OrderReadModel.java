package com.cartethyia.easyorange.order.domain.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderReadModel(
        Long id,
        String orderNo,
        Long buyerId,
        Long sellerId,
        Long productId,
        BigDecimal amount,
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
