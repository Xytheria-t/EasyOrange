package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.product.application.port.cache.ProductCachePort;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 商品缓存适配器测试 — 真实启用 Spring Cache AOP（ConcurrentMapCacheManager 替代 Redis），
 * 验证 {@code @Cacheable}/{@code @CacheEvict} 的命中/失效语义与 SpEL key 匹配。
 */
@SpringJUnitConfig(ProductCacheAdapterTest.TestConfig.class)
@DisplayName("商品缓存适配器（Spring Cache 注解式）测试")
class ProductCacheAdapterTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ProductCacheAdapter productCacheAdapter() {
            return new ProductCacheAdapter();
        }
    }

    @Autowired
    private CacheManager cacheManager;

    // @EnableCaching 生成 JDK 动态代理，按接口注入
    @Autowired
    private ProductCachePort cacheAdapter;

    @Autowired
    private ProductCacheEvictionPort evictionPort;

    /**
     * Spring TestContext 跨方法复用同一 context，缓存数据会泄漏到下一个用例。
     * 按具名缓存显式清空（动态缓存未创建前 {@code getCacheNames()} 为空集，清不到）。
     */
    @BeforeEach
    void clearCaches() {
        var cache = cacheManager.getCache(ProductCacheConstant.PRODUCT_INFO_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private static final String PRODUCT_ID = "1";

    private static ProductVO product(String id) {
        return ProductVO.builder()
                .id(id)
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("缓存命中：同一 key 二次读取不再执行 loader")
    void getProductCache_secondCall_loaderRunsOnce() {
        var calls = new AtomicInteger();

        ProductVO first = cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return product(PRODUCT_ID);
        });
        ProductVO second = cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return product(PRODUCT_ID);
        });

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("evict 后重新回源")
    void evictProductCache_forceReload() {
        var calls = new AtomicInteger();

        cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return product(PRODUCT_ID);
        });
        evictionPort.evictProductCache(PRODUCT_ID);
        cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return product(PRODUCT_ID);
        });

        assertThat(calls).hasValue(2);
    }

    @Test
    @DisplayName("productId 为 null：跳过缓存与回源，直接返回 null")
    void getProductCache_nullProductId_returnsNull() {
        var calls = new AtomicInteger();

        ProductVO result = cacheAdapter.getProductCache(null, () -> {
            calls.incrementAndGet();
            return product(PRODUCT_ID);
        });

        assertThat(result).isNull();
        assertThat(calls).hasValue(0);
    }

    @Test
    @DisplayName("loader 返回 null：不落缓存，每次读取都重新回源")
    void getProductCache_nullResult_notCached() {
        var calls = new AtomicInteger();

        cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return null;
        });
        cacheAdapter.getProductCache(PRODUCT_ID, () -> {
            calls.incrementAndGet();
            return null;
        });

        assertThat(calls).hasValue(2);
    }
}
