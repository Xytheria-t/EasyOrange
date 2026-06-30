package com.cartethyia.easyorange.product.domain.port;

public interface ProductCachePort<T> {

    T getProductCache(String productId);

    void setProductCache(String productId, T product);

    void evictProductCache(String productId);

    void evictProductListCache(String categoryId);
}
