package com.cartethyia.easyorange.order.domain.port.output;

public record OrderQueryCondition(
        String orderNo,
        Integer status,
        Long buyerId,
        Long sellerId,
        Integer pageNum,
        Integer pageSize
) {}
