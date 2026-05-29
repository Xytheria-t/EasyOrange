package com.cartethyia.easyorange.order.domain.port;

public record OrderQueryCondition(
        String orderNo,
        Integer status,
        Long buyerId,
        Long sellerId,
        Integer pageNum,
        Integer pageSize
) {}