package com.cartethyia.easyorange.order.domain.repository;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderReadRepository {

    Optional<Order> findById(Long id);

    List<Order> findByBuyerId(Long buyerId);

    List<Order> findBySellerId(Long sellerId);

    PageResult<Order> findPage(QueryOrderRequest request);
}
