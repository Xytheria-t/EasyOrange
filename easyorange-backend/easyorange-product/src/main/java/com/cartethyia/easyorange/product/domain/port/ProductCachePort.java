package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;

public interface ProductCachePort {

    ProductVO getProductCache(Long productId);

    void setProductCache(Long productId, ProductVO product);

    void evictProductCache(Long productId);

    void evictProductListCache(Long categoryId);
}
