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
 * 缓存三防在注解层收口：
 * <ul>
 *   <li><b>防穿透</b>：null 结果一并缓存（无 {@code unless}），查不存在 ID 不再每次打 DB；
 *       「ID 之后被创建」的一致性由写路径事件 evict 保证（{@code ProductDomainEventListener} 进程内 /
 *       {@code ProductEventConsumer} 跨实例，商品全写路径发事件）；</li>
 *   <li><b>防击穿</b>：{@code sync = true} 同 key 单飞重建，JVM 内并发未命中只有一个线程回源；
 *       跨实例残留少量并发由事件 evict + TTL 兜底；</li>
 *   <li><b>防雪崩</b>：TTL 随机抖动由 framework {@code JitterTtlRedisCacheWriter} 统一加，本层零感知。</li>
 * </ul>
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
