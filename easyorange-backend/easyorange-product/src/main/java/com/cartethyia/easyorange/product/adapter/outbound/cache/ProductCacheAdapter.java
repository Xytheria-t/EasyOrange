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
 * 三防在注解层收口：<b>防穿透</b>靠 null 结果一并缓存（无 {@code unless}），「ID 之后被创建」由写路径事件 evict 保证；
 * <b>防击穿</b>靠 {@code sync = true} 同 key 单飞——但单飞由 cache writer 承担、不是注解自己做的：writer 必须装配成
 * locking，且 {@code JitterTtlRedisCacheWriter} 必须透传带 loader 的那次 {@code get}，**任缺一条都静默失效**（不报错、
 * 不降级），两条由 {@code CacheSingleFlightIT} 拿真实 Redis 端到端钉住；
 * <b>防雪崩</b>的 TTL 随机抖动由 framework {@code JitterTtlRedisCacheWriter} 统一加，本层零感知。
 * <p>
 * Redis 故障由框架级 {@code CacheErrorHandler} fail-open，降级直查 DB。
 */
@Component
public class ProductCacheAdapter implements ProductCachePort, ProductCacheEvictionPort {

    @Override
    @Cacheable(
            cacheNames = ProductCacheConstant.PRODUCT_INFO_CACHE,
            key = "#productId",
            condition = "#productId != null",
            sync = true)
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
