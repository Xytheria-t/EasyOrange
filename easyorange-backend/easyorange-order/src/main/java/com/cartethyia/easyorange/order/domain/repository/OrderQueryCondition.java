package com.cartethyia.easyorange.order.domain.repository;

public record OrderQueryCondition(
        String orderNo,
        Integer status,
        Long buyerId,
        Long sellerId,
        Long productId,
        Integer pageNum,
        Integer pageSize
) {}
