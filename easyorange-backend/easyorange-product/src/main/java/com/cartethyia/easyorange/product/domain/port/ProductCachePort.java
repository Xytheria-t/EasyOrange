package com.cartethyia.easyorange.product.domain.port;

import java.util.Optional;

public interface ProductCachePort<T> {

    Optional<T> getProductCache(String productId);

    void setProductCache(String productId, T product);

    void evictProductCache(String productId);

    void evictProductListCache(String categoryId);
}
