package com.cartethyia.easyorange.product.adapter.outbound.cache;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品缓存适配器测试")
class ProductCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ProductCacheAdapter cacheAdapter;

    private ProductVO testProductVO;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheAdapter = new ProductCacheAdapter(redisTemplate);

        testProductVO = ProductVO.builder()
                .id(1L)
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("获取缓存 - 缓存命中")
    void getProductCache_cacheHit_shouldReturnProduct() {
        when(valueOperations.get(anyString())).thenReturn(testProductVO);

        ProductVO result = cacheAdapter.getProductCache(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("获取缓存 - 缓存未命中")
    void getProductCache_cacheMiss_shouldReturnNull() {
        when(valueOperations.get(anyString())).thenReturn(null);

        ProductVO result = cacheAdapter.getProductCache(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("获取缓存 - productId为null返回null")
    void getProductCache_nullProductId_shouldReturnNull() {
        ProductVO result = cacheAdapter.getProductCache(null);

        assertThat(result).isNull();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("设置缓存成功")
    void setProductCache_shouldSetCache() {
        cacheAdapter.setProductCache(1L, testProductVO);

        verify(valueOperations).set(anyString(), eq(testProductVO), anyLong(), any());
    }

    @Test
    @DisplayName("设置缓存 - 参数为null不操作")
    void setProductCache_nullParams_shouldNotOperate() {
        cacheAdapter.setProductCache(null, testProductVO);
        cacheAdapter.setProductCache(1L, null);

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("删除缓存成功")
    void evictProductCache_shouldDelete() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheAdapter.evictProductCache(1L);

        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("删除商品列表缓存")
    void evictProductListCache_shouldDelete() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheAdapter.evictProductListCache(1L);

        verify(redisTemplate).delete(anyString());
    }
}
