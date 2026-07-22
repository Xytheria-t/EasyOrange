package com.cartethyia.easyorange.product.domain.port;

public interface ProductCacheEvictionPort {

    void evictProductCache(String productId);

    void evictProductListCache(String categoryId);
}
