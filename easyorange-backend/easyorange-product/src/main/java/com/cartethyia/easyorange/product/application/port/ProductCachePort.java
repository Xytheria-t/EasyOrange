package com.cartethyia.easyorange.product.application.port;

import com.cartethyia.easyorange.product.application.query.ProductVO;

import java.util.Optional;

public interface ProductCachePort {

    Optional<ProductVO> getProductCache(String productId);

    void setProductCache(String productId, ProductVO product);

    void evictProductCache(String productId);

    void evictProductListCache(String categoryId);
}
