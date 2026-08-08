package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 读侧缓存端口（CQRS 查询侧）。写侧缓存失效见 {@link com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort}。
 * 回源 loader 由 {@code MultiLevelCache} 单飞执行（Caffeine 原子 + 可选 Redisson 跨节点锁）。
 */
public interface ProductCachePort {

    Optional<ProductVO> getProductCache(String productId, Supplier<ProductVO> loader);
}
