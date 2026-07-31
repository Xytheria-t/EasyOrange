package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品缓存适配器测试")
class ProductCacheAdapterTest {

    @Mock
    private MultiLevelCache multiLevelCache;

    private ProductCacheAdapter cacheAdapter;

    private ProductVO testProductVO;

    private static final String PRODUCT_ID = "1";

    @BeforeEach
    void setUp() {
        cacheAdapter = new ProductCacheAdapter(multiLevelCache);

        testProductVO = ProductVO.builder()
                .id(PRODUCT_ID)
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("获取缓存 - 缓存命中返回商品")
    void getProductCache_cacheHit_shouldReturnProduct() {
        when(multiLevelCache.get(anyString(), eq(ProductVO.class), any())).thenReturn(testProductVO);

        Optional<ProductVO> result = cacheAdapter.getProductCache(PRODUCT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("获取缓存 - 缓存未命中返回空")
    void getProductCache_cacheMiss_shouldReturnEmpty() {
        when(multiLevelCache.get(anyString(), eq(ProductVO.class), any())).thenReturn(null);

        Optional<ProductVO> result = cacheAdapter.getProductCache(PRODUCT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("获取缓存 - productId为null返回空")
    void getProductCache_nullProductId_shouldReturnEmpty() {
        Optional<ProductVO> result = cacheAdapter.getProductCache(null);

        assertThat(result).isEmpty();
        verify(multiLevelCache, never()).get(anyString(), any(), any());
    }

    @Test
    @DisplayName("设置缓存成功")
    void setProductCache_shouldSetCache() {
        cacheAdapter.setProductCache(PRODUCT_ID, testProductVO);

        verify(multiLevelCache).put(eq(ProductCacheConstant.infoKey(PRODUCT_ID)), eq(testProductVO));
    }

    @Test
    @DisplayName("设置缓存 - 参数为null不操作")
    void setProductCache_nullParams_shouldNotOperate() {
        cacheAdapter.setProductCache(null, testProductVO);
        cacheAdapter.setProductCache(PRODUCT_ID, null);

        verify(multiLevelCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("删除缓存成功")
    void evictProductCache_shouldDelete() {
        doNothing().when(multiLevelCache).evict(anyString());

        cacheAdapter.evictProductCache(PRODUCT_ID);

        verify(multiLevelCache).evict(ProductCacheConstant.infoKey(PRODUCT_ID));
    }

    @Test
    @DisplayName("删除商品列表缓存")
    void evictProductListCache_shouldDelete() {
        doNothing().when(multiLevelCache).evict(anyString());

        cacheAdapter.evictProductListCache(PRODUCT_ID);

        verify(multiLevelCache).evict(ProductCacheConstant.listKey(PRODUCT_ID));
    }
}
