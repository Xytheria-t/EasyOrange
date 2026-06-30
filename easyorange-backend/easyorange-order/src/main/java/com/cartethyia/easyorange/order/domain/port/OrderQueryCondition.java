package com.cartethyia.easyorange.order.domain.port;

public record OrderQueryCondition(
        String orderNo,
        Integer status,
        String buyerId,
        String sellerId,
        Integer pageNum,
        Integer pageSize
) {}