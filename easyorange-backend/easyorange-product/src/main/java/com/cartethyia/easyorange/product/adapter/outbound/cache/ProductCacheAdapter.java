package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.port.cache.ProductCachePort;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import java.util.function.Supplier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 商品缓存适配器 — Spring Cache 注解式（纯 Redis 单层，见 framework {@code RedisCacheConfig}）。
 * <p>
 * 读：{@link #getProductCache} 缓存商品 VO（TTL 统一由 {@code easyorange.cache.default-ttl} 控制，
 * 未命中执行 loader 回源，loader 返回 null 不落缓存）；写路径失效由 {@link #evictProductCache}
 * 显式触发（商品领域事件 / MQ 事件消费）。Redis 故障由框架级 {@code CacheErrorHandler} fail-open，
 * 降级直查 DB。
 */
@Component
public class ProductCacheAdapter implements ProductCachePort, ProductCacheEvictionPort {

    @Override
    @Cacheable(
            cacheNames = ProductCacheConstant.PRODUCT_INFO_CACHE,
            key = "#productId",
            condition = "#productId != null",
            unless = "#result == null")
    public ProductVO getProductCache(String productId, Supplier<ProductVO> loader) {
        return productId == null ? null : loader.get();
    }

    @Override
    @CacheEvict(
            cacheNames = ProductCacheConstant.PRODUCT_INFO_CACHE,
            key = "#productId",
            condition = "#productId != null")
    public void evictProductCache(String productId) {
        // 失效由 @CacheEvict 代理执行，空实现仅满足端口契约
    }
}
