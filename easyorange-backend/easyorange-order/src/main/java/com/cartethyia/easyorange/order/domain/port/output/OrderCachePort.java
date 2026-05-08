package com.cartethyia.easyorange.order.domain.port.output;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.response.OrderVO;

import java.util.Optional;

public interface OrderCachePort {

    Optional<PageResult<OrderVO>> getOrderList(String cacheKey);

    void putOrderList(String cacheKey, PageResult<OrderVO> pageResult);

    void evictOrderList(String cacheKey);

    Optional<OrderVO> getOrderDetail(Long orderId);

    void putOrderDetail(Long orderId, OrderVO orderVO);

    void evictOrderDetail(Long orderId);

    void evictBuyerOrders(Long buyerId);

    void evictSellerOrders(Long sellerId);

    void evictOrderCache(Long buyerId, Long sellerId);

    String buildOrderListKey(Long userId, Integer status, Integer pageNum, Integer pageSize);

    String buildOrderListKey(Long userId, Integer status);
}
