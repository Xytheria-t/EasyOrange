package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import java.util.function.Supplier;

/**
 * 读侧缓存端口（CQRS 查询侧）。写侧缓存失效见 {@link com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort}。
 * 实现基于 Spring Cache {@code @Cacheable}（纯 Redis 单层 + 短 TTL），未命中时执行 loader 回源。
 */
public interface ProductCachePort {

    /**
     * @param productId 商品 ID（null 直接返回 null，不触缓存）
     * @param loader    缓存未命中时的回源逻辑
     * @return 缓存的商品 VO；商品不存在或 loader 返回 null 时为 null（null 不落缓存）
     */
    ProductVO getProductCache(String productId, Supplier<ProductVO> loader);
}
