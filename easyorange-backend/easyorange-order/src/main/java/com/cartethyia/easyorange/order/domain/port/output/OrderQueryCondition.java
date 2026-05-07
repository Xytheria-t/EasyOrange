package com.cartethyia.easyorange.order.domain.port.output;

public record OrderQueryCondition(
        String orderNo,
        Integer status,
        Long buyerId,
        Long sellerId,
        Long productId,
        Integer pageNum,
        Integer pageSize
) {}
