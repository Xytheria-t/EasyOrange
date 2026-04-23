package com.cartethyia.easyorange.product.application.cache;

import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品缓存服务测试
 * 
 * @author cartethyia
 * @date 2026/04/23
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("商品缓存服务测试")
class ProductCacheServiceTest {

    @Autowired
    private ProductCacheService productCacheService;

    private Long testProductId;
    private ProductVO testProduct;

    @BeforeEach
    void setUp() {
        testProductId = 999999L;
        testProduct = ProductVO.builder()
                .id(testProductId)
                .title("测试商品")
                .price(new BigDecimal("99.99"))
                .originalPrice(new BigDecimal("199.99"))
                .stock(100)
                .status(1)
                .build();
    }

    @Test
    @DisplayName("设置和获取商品缓存")
    void testSetAndGetProductCache() {
        productCacheService.setProductCache(testProductId, testProduct);
        
        ProductVO cachedProduct = productCacheService.getProductCache(testProductId);
        
        assertThat(cachedProduct).isNotNull();
        assertThat(cachedProduct.getId()).isEqualTo(testProductId);
        assertThat(cachedProduct.getTitle()).isEqualTo("测试商品");
    }

    @Test
    @DisplayName("获取不存在的商品缓存")
    void testGetNonExistentCache() {
        ProductVO cachedProduct = productCacheService.getProductCache(999998L);
        
        assertThat(cachedProduct).isNull();
    }

    @Test
    @DisplayName("检查商品缓存是否存在")
    void testHasProductCache() {
        productCacheService.setProductCache(testProductId, testProduct);
        
        Boolean hasCache = productCacheService.hasProductCache(testProductId);
        
        assertThat(hasCache).isTrue();
    }

    @Test
    @DisplayName("删除商品缓存")
    void testDeleteProductCache() {
        productCacheService.setProductCache(testProductId, testProduct);
        
        productCacheService.deleteProductCache(testProductId);
        
        ProductVO cachedProduct = productCacheService.getProductCache(testProductId);
        assertThat(cachedProduct).isNull();
    }

    @Test
    @DisplayName("批量删除商品缓存")
    void testDeleteProductBatchCache() {
        List<Long> productIds = List.of(1000001L, 1000002L, 1000003L);
        
        for (Long productId : productIds) {
            ProductVO product = ProductVO.builder()
                    .id(productId)
                    .title("测试商品" + productId)
                    .price(new BigDecimal("99.99"))
                    .build();
            productCacheService.setProductCache(productId, product);
        }
        
        productCacheService.deleteProductBatchCache(productIds);
        
        for (Long productId : productIds) {
            ProductVO cachedProduct = productCacheService.getProductCache(productId);
            assertThat(cachedProduct).isNull();
        }
    }

    @Test
    @DisplayName("性能测试：大量缓存读写")
    void testPerformance() {
        int count = 1000;
        List<Long> productIds = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Long productId = 2000000L + i;
            productIds.add(productId);
            
            ProductVO product = ProductVO.builder()
                    .id(productId)
                    .title("性能测试商品" + i)
                    .price(new BigDecimal("99.99"))
                    .build();
            
            productCacheService.setProductCache(productId, product);
        }
        
        long setEndTime = System.currentTimeMillis();
        
        for (Long productId : productIds) {
            ProductVO cachedProduct = productCacheService.getProductCache(productId);
            assertThat(cachedProduct).isNotNull();
        }
        
        long getEndTime = System.currentTimeMillis();
        
        System.out.println("设置 " + count + " 个缓存耗时：" + (setEndTime - startTime) + "ms");
        System.out.println("获取 " + count + " 个缓存耗时：" + (getEndTime - setEndTime) + "ms");
        System.out.println("总耗时：" + (getEndTime - startTime) + "ms");
        System.out.println("平均每个缓存操作耗时：" + ((getEndTime - startTime) / count) + "ms");
    }
}
