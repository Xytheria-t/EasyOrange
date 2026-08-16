package com.cartethyia.easyorange.order.domain.port;

import com.cartethyia.easyorange.common.result.PageResult;
import java.util.Optional;

public interface OrderCachePort<T> {

    Optional<PageResult<T>> getOrderList(String cacheKey);

    void putOrderList(String cacheKey, PageResult<T> pageResult);

    /** 失效买家与卖家双方的订单列表缓存。 */
    void evictOrderCache(String buyerId, String sellerId);

    String buildOrderListKey(String userId, String status, Integer pageNum, Integer pageSize);
}
