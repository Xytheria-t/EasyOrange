package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        Optional<ProductVO> result = cacheAdapter.getProductCache(PRODUCT_ID, this::testLoader);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(PRODUCT_ID);
        verify(multiLevelCache).get(eq(ProductCacheConstant.infoKey(PRODUCT_ID)), eq(ProductVO.class), any());
    }

    @Test
    @DisplayName("获取缓存 - 缓存未命中时经 loader 回源")
    void getProductCache_cacheMiss_shouldLoad() {
        when(multiLevelCache.get(anyString(), eq(ProductVO.class), any())).thenAnswer(invocation -> {
            Supplier<ProductVO> loader = invocation.getArgument(2);
            return loader.get();
        });

        Optional<ProductVO> result = cacheAdapter.getProductCache(PRODUCT_ID, this::testLoader);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("获取缓存 - productId为null返回空")
    void getProductCache_nullProductId_shouldReturnEmpty() {
        Optional<ProductVO> result = cacheAdapter.getProductCache(null, this::testLoader);

        assertThat(result).isEmpty();
        verify(multiLevelCache, never()).get(anyString(), any(), any());
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

    private ProductVO testLoader() {
        return testProductVO;
    }
}
