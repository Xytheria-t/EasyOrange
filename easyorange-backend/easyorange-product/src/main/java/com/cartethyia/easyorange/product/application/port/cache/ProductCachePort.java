package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import java.util.Optional;

/**
 * 读侧缓存端口（CQRS 查询侧）。写侧缓存失效见 {@link com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort}。
 */
public interface ProductCachePort {

    Optional<ProductVO> getProductCache(String productId);

    void setProductCache(String productId, ProductVO product);
}
