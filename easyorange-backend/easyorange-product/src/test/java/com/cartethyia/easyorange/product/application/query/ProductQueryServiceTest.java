package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品查询服务测试")
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductReadModelAssembler readModelAssembler;

    @Mock
    private ProductCachePort productCachePort;

    private ProductQueryService queryService;

    private Product testProduct;
    private ProductVO testProductVO;

    @BeforeEach
    void setUp() {
        queryService = new ProductQueryService(productRepository, productQueryRepository, readModelAssembler, productCachePort);

        testProduct = Product.create(
                SellerId.of("1"),
                CategoryId.of("2"),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")),
                null,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        ).product().assignId("1");

        testProductVO = ProductVO.builder()
                .id("1")
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("缓存命中时直接返回缓存数据")
    void getProductById_cacheHit_shouldReturnCached() {
        when(productCachePort.getProductCache("1")).thenReturn(testProductVO);

        ProductVO result = queryService.getProductById("1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("1");
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("缓存未命中时从数据库查询并写入缓存")
    void getProductById_cacheMiss_shouldQueryDbAndSetCache() {
        when(productCachePort.getProductCache("1")).thenReturn(null);
        when(productRepository.findById(ProductId.of("1"))).thenReturn(Optional.of(testProduct));
        when(productQueryRepository.findImagesByProductIds(any())).thenReturn(List.of());
        when(productQueryRepository.findCategoriesByIds(any())).thenReturn(List.of());
        when(productQueryRepository.findDetailsByProductIds(any())).thenReturn(List.of());
        when(productQueryRepository.findSellersByIds(any())).thenReturn(List.of());
        when(readModelAssembler.toProductVO(eq(testProduct), any(), any(), any(), any())).thenReturn(testProductVO);

        ProductVO result = queryService.getProductById("1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("1");
        verify(productCachePort).setProductCache(eq("1"), eq(testProductVO));
    }

    @Test
    @DisplayName("查询不存在的商品应抛出异常")
    void getProductById_notFound_shouldThrow() {
        when(productCachePort.getProductCache("999")).thenReturn(null);
        when(productRepository.findById(ProductId.of("999"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getProductById("999"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("批量查询商品ID为空时返回空列表")
    void getProductsByIds_emptyIds_shouldReturnEmpty() {
        List<ProductVO> result = queryService.getProductsByIds(List.of());

        assertThat(result).isEmpty();
        verify(productRepository, never()).findByIds(any());
    }
}
