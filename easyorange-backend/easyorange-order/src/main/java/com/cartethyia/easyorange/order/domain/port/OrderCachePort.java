package com.cartethyia.easyorange.order.domain.port;

import com.cartethyia.easyorange.common.result.PageResult;
import java.util.Optional;

public interface OrderCachePort<T> {

    Optional<PageResult<T>> getOrderList(String cacheKey);

    void putOrderList(String cacheKey, PageResult<T> pageResult);

    void evictOrderList(String cacheKey);

    void evictBuyerOrders(String buyerId);

    void evictSellerOrders(String sellerId);

    void evictOrderCache(String buyerId, String sellerId);

    String buildOrderListKey(String userId, String status, Integer pageNum, Integer pageSize);

    String buildOrderListKey(String userId, String status);
}
