package com.cartethyia.easyorange.product.application.port;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;

import java.util.Optional;

public interface ProductCachePort extends ProductCacheEvictionPort {

    Optional<ProductVO> getProductCache(String productId);

    void setProductCache(String productId, ProductVO product);
}
