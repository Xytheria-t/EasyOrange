package com.cartethyia.easyorange.order.domain.port;

import com.cartethyia.easyorange.common.result.PageResult;

import java.util.Optional;

public interface OrderCachePort<T> {

    Optional<PageResult<T>> getOrderList(String cacheKey);

    void putOrderList(String cacheKey, PageResult<T> pageResult);

    void evictOrderList(String cacheKey);

    Optional<T> getOrderDetail(Long orderId);

    void putOrderDetail(Long orderId, T orderVO);

    void evictOrderDetail(Long orderId);

    void evictBuyerOrders(Long buyerId);

    void evictSellerOrders(Long sellerId);

    void evictOrderCache(Long buyerId, Long sellerId);

    String buildOrderListKey(Long userId, Integer status, Integer pageNum, Integer pageSize);

    String buildOrderListKey(Long userId, Integer status);
}