package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.PriceAdjustedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.event.PriceAdjustedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品命令服务 - AI托管寄售测试")
class ProductCommandServiceConsignmentTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCachePort productCachePort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private ProductAuditLogRepository productAuditLogRepository;

    private ProductCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ProductCommandService(
                productRepository, productCachePort, domainEventPublisher, productAuditLogRepository);
    }

    @Test
    @DisplayName("AI托管模式 + 正确底价 → 创建成功，字段正确")
    void createProduct_withAiManagedAndFloorPrice_shouldSucceed() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                return p.assignId(42L);
            });

            CreateProductCommand command = CreateProductCommand.builder()
                    .categoryId(2L)
                    .name("测试商品")
                    .price(new BigDecimal("100"))
                    .stock(10)
                    .conditionLevel(1)
                    .location("北京")
                    .contactMethod("微信")
                    .description("描述")
                    .imageUrls(java.util.List.of("http://img/1.jpg"))
                    .consignmentMode(1)
                    .floorPrice(new BigDecimal("50"))
                    .build();

            Long productId = commandService.createProduct(command);

            assertThat(productId).isEqualTo(42L);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());
            Product saved = productCaptor.getValue();

            assertThat(saved.getConsignmentMode()).isEqualTo(ConsignmentMode.AI_MANAGED);
            assertThat(saved.getFloorPrice()).isNotNull();
            assertThat(saved.getFloorPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(saved.getCurrentPriceLevel()).isEqualTo(0);

            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("AI托管模式 + 底价为null → 抛出异常")
    void createProduct_withAiManagedAndNullFloorPrice_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            CreateProductCommand command = CreateProductCommand.builder()
                    .categoryId(2L)
                    .name("测试商品")
                    .price(new BigDecimal("100"))
                    .stock(10)
                    .conditionLevel(1)
                    .location("北京")
                    .contactMethod("微信")
                    .description("描述")
                    .imageUrls(java.util.List.of("http://img/1.jpg"))
                    .consignmentMode(1)
                    .floorPrice(null)
                    .build();

            assertThatThrownBy(() -> commandService.createProduct(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AI托管模式必须设置底价");

            verify(productRepository, never()).save(any());
            verify(domainEventPublisher, never()).publish(any());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("AI托管模式 + 底价高于标价 → 抛出异常")
    void createProduct_withAiManagedAndFloorPriceAbovePrice_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            CreateProductCommand command = CreateProductCommand.builder()
                    .categoryId(2L)
                    .name("测试商品")
                    .price(new BigDecimal("100"))
                    .stock(10)
                    .conditionLevel(1)
                    .location("北京")
                    .contactMethod("微信")
                    .description("描述")
                    .imageUrls(java.util.List.of("http://img/1.jpg"))
                    .consignmentMode(1)
                    .floorPrice(new BigDecimal("150"))
                    .build();

            assertThatThrownBy(() -> commandService.createProduct(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("底价不能高于标价");

            verify(productRepository, never()).save(any());
            verify(domainEventPublisher, never()).publish(any());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("手动模式 + floorPrice=null → 创建成功，consignmentMode=MANUAL")
    void createProduct_withManualModeAndNoFloorPrice_shouldSucceed() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                return p.assignId(42L);
            });

            CreateProductCommand command = CreateProductCommand.builder()
                    .categoryId(2L)
                    .name("测试商品")
                    .price(new BigDecimal("100"))
                    .stock(10)
                    .conditionLevel(1)
                    .location("北京")
                    .contactMethod("微信")
                    .description("描述")
                    .imageUrls(java.util.List.of("http://img/1.jpg"))
                    .consignmentMode(0)
                    .floorPrice(null)
                    .build();

            Long productId = commandService.createProduct(command);

            assertThat(productId).isEqualTo(42L);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());
            Product saved = productCaptor.getValue();

            assertThat(saved.getConsignmentMode()).isEqualTo(ConsignmentMode.MANUAL);
            assertThat(saved.getFloorPrice()).isNull();

            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("adjustPrice → 价格更新、事件发布、缓存失效")
    void adjustPrice_shouldUpdatePriceAndPublishEventAndEvictCache() {
        // Arrange: create an AI-managed online product
        TestSecurityUtil.setSecurityContext(1L);
        try {
            ProductCreatedResult created = Product.create(
                    com.cartethyia.easyorange.product.domain.valueobject.SellerId.of(1L),
                    com.cartethyia.easyorange.product.domain.valueobject.CategoryId.of(2L),
                    com.cartethyia.easyorange.product.domain.valueobject.ProductTitle.of("测试商品"),
                    Money.of(new BigDecimal("100")),
                    null,
                    Money.of(new BigDecimal("50")),
                    ConsignmentMode.AI_MANAGED,
                    com.cartethyia.easyorange.product.domain.valueobject.StockQuantity.of(10),
                    ConditionLevel.NEW,
                    com.cartethyia.easyorange.product.domain.valueobject.TradeLocation.of("北京"),
                    com.cartethyia.easyorange.product.domain.valueobject.ContactMethod.of("微信"),
                    com.cartethyia.easyorange.product.domain.valueobject.ProductDescription.of("描述"),
                    com.cartethyia.easyorange.product.domain.valueobject.ImageSet.of(java.util.List.of("http://img/1.jpg"))
            );
            Product aiManagedProduct = created.product().assignId(1L).putOnline();

            when(productRepository.findById(ProductId.of(1L))).thenReturn(Optional.of(aiManagedProduct));

            commandService.adjustPrice(1L, 1);

            // Verify price was updated to 95 (100 * 0.95)
            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).update(productCaptor.capture());
            Product updated = productCaptor.getValue();
            assertThat(updated.getPrice().value()).isEqualByComparingTo(new BigDecimal("95.00"));
            assertThat(updated.getCurrentPriceLevel()).isEqualTo(1);

            // Verify event was published
            verify(domainEventPublisher).publish(any(PriceAdjustedEvent.class));

            // Verify cache was evicted
            verify(productCachePort).evictProductCache(1L);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("adjustPrice 商品不存在 → 抛出异常")
    void adjustPrice_whenProductNotFound_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.adjustPrice(999L, 1))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
            verify(productCachePort, never()).evictProductCache(anyLong());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
