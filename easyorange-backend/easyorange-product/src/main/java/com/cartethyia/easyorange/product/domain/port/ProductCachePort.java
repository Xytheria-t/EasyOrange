package com.cartethyia.easyorange.product.domain.port;

public interface ProductCachePort<T> {

    T getProductCache(Long productId);

    void setProductCache(Long productId, T product);

    void evictProductCache(Long productId);

    void evictProductListCache(Long categoryId);
}
