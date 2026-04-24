package com.cartethyia.easyorange.product.application.cache;

import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("商品缓存服务测试")
class ProductCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ProductCacheService productCacheService;

    private Long testProductId;
    private ProductVO testProduct;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        productCacheService = new ProductCacheService(redisTemplate);

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
        when(valueOperations.get("product:detail:" + testProductId)).thenReturn(testProduct);

        productCacheService.setProductCache(testProductId, testProduct);
        ProductVO cachedProduct = productCacheService.getProductCache(testProductId);

        assertThat(cachedProduct).isNotNull();
        assertThat(cachedProduct.getId()).isEqualTo(testProductId);
        assertThat(cachedProduct.getTitle()).isEqualTo("测试商品");

        verify(valueOperations).set(eq("product:detail:" + testProductId), eq(testProduct), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("获取不存在的商品缓存")
    void testGetNonExistentCache() {
        when(valueOperations.get("product:detail:999998")).thenReturn(null);

        ProductVO cachedProduct = productCacheService.getProductCache(999998L);

        assertThat(cachedProduct).isNull();
    }

    @Test
    @DisplayName("检查商品缓存是否存在")
    void testHasProductCache() {
        when(redisTemplate.hasKey("product:detail:" + testProductId)).thenReturn(true);

        productCacheService.setProductCache(testProductId, testProduct);
        Boolean hasCache = productCacheService.hasProductCache(testProductId);

        assertThat(hasCache).isTrue();
    }

    @Test
    @DisplayName("删除商品缓存")
    void testDeleteProductCache() {
        when(redisTemplate.delete("product:detail:" + testProductId)).thenReturn(true);

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
            when(redisTemplate.delete("product:detail:" + productId)).thenReturn(true);
        }

        productCacheService.deleteProductBatchCache(productIds);

        verify(redisTemplate, times(3)).delete(any(String.class));
    }

    @Test
    @DisplayName("null productId 不执行操作")
    void testNullProductId_skipsOperation() {
        productCacheService.setProductCache(null, testProduct);
        productCacheService.getProductCache(null);
        productCacheService.deleteProductCache(null);
        productCacheService.hasProductCache(null);

        verify(valueOperations, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }
}