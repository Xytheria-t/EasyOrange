package com.cartethyia.easyorange.product.application.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.application.port.cache.ProductCachePort;
import com.cartethyia.easyorange.product.application.port.cache.SellerCachePort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品查询处理器测试")
class ProductQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductReadModelAssembler readModelAssembler;

    @Mock
    private ProductCachePort productCachePort;

    @Mock
    private SellerCachePort sellerCachePort;

    private ProductQueryHandler queryHandler;

    private Product testProduct;
    private ProductVO testProductVO;

    @BeforeEach
    void setUp() {
        queryHandler = new ProductQueryHandler(
                productRepository, productQueryRepository, readModelAssembler, productCachePort, sellerCachePort);

        testProduct = Product.create(new ProductCreateSpec(
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
                        ImageSet.of(List.of("http://img/1.jpg"))))
                .aggregate()
                .assignId("1");

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
        when(productCachePort.getProductCache(eq("1"), any())).thenReturn(Optional.of(testProductVO));

        ProductVO result = queryHandler.getProductById("1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("1");
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("缓存未命中时经回源 loader 查询数据库")
    void getProductById_cacheMiss_shouldLoadViaLoader() {
        when(productCachePort.getProductCache(eq("1"), any())).thenAnswer(invocation -> {
            Supplier<ProductVO> loader = invocation.getArgument(1);
            return Optional.ofNullable(loader.get());
        });
        when(productRepository.findById(ProductId.of("1"))).thenReturn(Optional.of(testProduct));
        when(productQueryRepository.findImagesByProductIds(any())).thenReturn(List.of());
        when(productQueryRepository.findCategoriesByIds(any())).thenReturn(List.of());
        when(productQueryRepository.findDetailsByProductIds(any())).thenReturn(List.of());
        when(sellerCachePort.getSellers(any())).thenReturn(Map.of());
        when(readModelAssembler.toProductVO(eq(testProduct), any())).thenReturn(testProductVO);

        ProductVO result = queryHandler.getProductById("1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("1");
        verify(productRepository).findById(ProductId.of("1"));
    }

    @Test
    @DisplayName("查询不存在的商品应抛出异常")
    void getProductById_notFound_shouldThrow() {
        when(productCachePort.getProductCache(eq("999"), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryHandler.getProductById("999")).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("批量查询商品ID为空时返回空列表")
    void getProductsByIds_emptyIds_shouldReturnEmpty() {
        List<ProductVO> result = queryHandler.getProductsByIds(List.of());

        assertThat(result).isEmpty();
        verify(productRepository, never()).findByIds(any());
    }
}
