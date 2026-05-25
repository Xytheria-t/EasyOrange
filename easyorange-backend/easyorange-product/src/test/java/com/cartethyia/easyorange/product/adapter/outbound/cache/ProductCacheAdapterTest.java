package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.framework.bloom.RedisBitmapBloomFilter;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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

    @Mock
    private RedisBitmapBloomFilter bloomFilter;

    private ProductCacheAdapter cacheAdapter;

    private ProductVO testProductVO;

    private static final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        cacheAdapter = new ProductCacheAdapter(multiLevelCache, bloomFilter);

        testProductVO = ProductVO.builder()
                .id(PRODUCT_ID)
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("获取缓存 - Bloom Filter 命中且缓存命中")
    void getProductCache_bloomHitAndCacheHit_shouldReturnProduct() {
        when(bloomFilter.mightContain(anyString(), eq(PRODUCT_ID.toString()))).thenReturn(true);
        when(multiLevelCache.get(anyString(), eq(ProductVO.class), any())).thenReturn(testProductVO);

        ProductVO result = cacheAdapter.getProductCache(PRODUCT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("获取缓存 - Bloom Filter 未命中返回 null")
    void getProductCache_bloomMiss_shouldReturnNull() {
        when(bloomFilter.mightContain(anyString(), eq(PRODUCT_ID.toString()))).thenReturn(false);

        ProductVO result = cacheAdapter.getProductCache(PRODUCT_ID);

        assertThat(result).isNull();
        verify(multiLevelCache, never()).get(anyString(), any(), any());
    }

    @Test
    @DisplayName("获取缓存 - productId为null返回null")
    void getProductCache_nullProductId_shouldReturnNull() {
        ProductVO result = cacheAdapter.getProductCache(null);

        assertThat(result).isNull();
        verify(bloomFilter, never()).mightContain(anyString(), anyString());
    }

    @Test
    @DisplayName("获取缓存 - 缓存未命中返回null")
    void getProductCache_cacheMiss_shouldReturnNull() {
        when(bloomFilter.mightContain(anyString(), eq(PRODUCT_ID.toString()))).thenReturn(true);
        when(multiLevelCache.get(anyString(), eq(ProductVO.class), any())).thenReturn(null);

        ProductVO result = cacheAdapter.getProductCache(PRODUCT_ID);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("设置缓存成功")
    void setProductCache_shouldSetCache() {
        cacheAdapter.setProductCache(PRODUCT_ID, testProductVO);

        verify(bloomFilter).put(eq(ProductCacheConstant.PRODUCT_BLOOM_KEY), eq(PRODUCT_ID.toString()));
        verify(multiLevelCache).put(eq(ProductCacheConstant.infoKey(PRODUCT_ID)), eq(testProductVO));
    }

    @Test
    @DisplayName("设置缓存 - 参数为null不操作")
    void setProductCache_nullParams_shouldNotOperate() {
        cacheAdapter.setProductCache(null, testProductVO);
        cacheAdapter.setProductCache(PRODUCT_ID, null);

        verify(bloomFilter, never()).put(anyString(), anyString());
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
        doNothing().when(multiLevelCache).evictL2(anyString());

        cacheAdapter.evictProductListCache(1L);

        verify(multiLevelCache).evictL2(ProductCacheConstant.listKey(1L));
    }
}