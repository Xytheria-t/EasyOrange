package com.cartethyia.easyorange.product.adapter.inbound.job;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductPriceAdjustTaskTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCommandService productCommandService;

    private ProductPriceAdjustTask task;

    @BeforeEach
    void setUp() {
        task = new ProductPriceAdjustTask(productRepository, productCommandService);
    }

    private Product createProduct(Long id, int daysAgo, int currentLevel) {
        return Product.reconstitute(
                ProductId.of(id),
                SellerId.of(1L),
                CategoryId.of(1L),
                ProductTitle.of("测试商品" + id),
                Money.of(new BigDecimal("100.00")),
                null,
                Money.of(new BigDecimal("50.00")),
                ConsignmentMode.AI_MANAGED,
                LocalDateTime.now().minusDays(daysAgo),
                currentLevel,
                StockQuantity.of(1),
                Version.INITIAL,
                ProductStatus.ONLINE,
                0,
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg")),
                TagSet.empty(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("应根据上架时间进行阶梯降价")
    void shouldAdjustPricesBasedOnListedAt() {
        // Product 1: listedAt 3天前, level=0 → expectedLevel=0, 不需要降价
        // Product 2: listedAt 5天前, level=0 → expectedLevel=1, 需要降到 level 1
        // Product 3: listedAt 8天前, level=1 → expectedLevel=3, 需要降到 level 3
        Product product1 = createProduct(1L, 3, 0);
        Product product2 = createProduct(2L, 5, 0);
        Product product3 = createProduct(3L, 8, 1);

        given(productRepository.findAiManagedOnline())
                .willReturn(List.of(product1, product2, product3));

        task.executePriceAdjustment();

        verify(productCommandService).adjustPrice(2L, 1);
        verify(productCommandService).adjustPrice(3L, 3);
        verify(productCommandService, times(2)).adjustPrice(anyLong(), anyInt());
    }

    @Test
    @DisplayName("所有商品已在最高阶梯时不进行降价")
    void allProductsAtMaxLevel_shouldNotAdjust() {
        // Product 1: listedAt 8天前, level=3 → expectedLevel=3, 不需要降价
        // Product 2: listedAt 9天前, level=3 → expectedLevel=3, 不需要降价
        Product product1 = createProduct(1L, 8, 3);
        Product product2 = createProduct(2L, 9, 3);

        given(productRepository.findAiManagedOnline())
                .willReturn(List.of(product1, product2));

        task.executePriceAdjustment();

        verify(productCommandService, never()).adjustPrice(anyLong(), anyInt());
    }

    @Test
    @DisplayName("单个商品降价失败不应影响其他商品")
    void singleFailure_shouldNotAffectOthers() {
        // Product 1: listedAt 5天前, level=0 → expectedLevel=1, adjustPrice 会抛出异常
        // Product 2: listedAt 8天前, level=1 → expectedLevel=3, 正常降价
        Product product1 = createProduct(1L, 5, 0);
        Product product2 = createProduct(2L, 8, 1);

        given(productRepository.findAiManagedOnline())
                .willReturn(List.of(product1, product2));
        willThrow(new RuntimeException("模拟降价失败"))
                .given(productCommandService).adjustPrice(1L, 1);

        task.executePriceAdjustment();

        verify(productCommandService).adjustPrice(1L, 1);
        verify(productCommandService).adjustPrice(2L, 3);
    }

    @Test
    @DisplayName("没有 AI 托管商品时不应进行降价操作")
    void noProducts_shouldDoNothing() {
        given(productRepository.findAiManagedOnline())
                .willReturn(List.of());

        task.executePriceAdjustment();

        verify(productCommandService, never()).adjustPrice(anyLong(), anyInt());
    }
}
