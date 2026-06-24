package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.dto.UpdateProductCommand;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品命令服务测试")
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCachePort productCachePort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private ProductAuditLogRepository productAuditLogRepository;

    private ProductCommandService commandService;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        commandService = new ProductCommandService(productRepository, productCachePort, domainEventPublisher, productAuditLogRepository);

        ProductCreatedResult created = Product.create(
                com.cartethyia.easyorange.product.domain.valueobject.SellerId.of(1L),
                com.cartethyia.easyorange.product.domain.valueobject.CategoryId.of(2L),
                com.cartethyia.easyorange.product.domain.valueobject.ProductTitle.of("测试商品"),
                com.cartethyia.easyorange.common.domain.Money.of(new BigDecimal("100")),
                null,
                null,
                ConsignmentMode.MANUAL,
                com.cartethyia.easyorange.product.domain.valueobject.StockQuantity.of(10),
                ConditionLevel.NEW,
                com.cartethyia.easyorange.product.domain.valueobject.TradeLocation.of("北京"),
                com.cartethyia.easyorange.product.domain.valueobject.ContactMethod.of("微信"),
                com.cartethyia.easyorange.product.domain.valueobject.ProductDescription.of("描述"),
                com.cartethyia.easyorange.product.domain.valueobject.ImageSet.of(java.util.List.of("http://img/1.jpg"))
        );
        existingProduct = created.product().assignId(1L);
        existingProduct = existingProduct.putOnline();
    }

    @Test
    @DisplayName("创建商品应调用仓储保存")
    void createProduct_shouldSaveToRepository() {
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
                    .build();

            Long productId = commandService.createProduct(command);

            assertThat(productId).isEqualTo(42L);
            verify(productRepository).save(any(Product.class));
            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("更新商品后应使缓存失效")
    void updateProduct_shouldEvictCache() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

            UpdateProductCommand command = UpdateProductCommand.builder()
                    .id(1L)
                    .name("新名称")
                    .price(new BigDecimal("200"))
                    .build();

            commandService.updateProduct(command);

            verify(productCachePort).evictProductCache(1L);
            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("更新不存在的商品应抛出异常")
    void updateProduct_whenNotFound_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

            UpdateProductCommand command = UpdateProductCommand.builder()
                    .id(999L)
                    .name("新名称")
                    .build();

            assertThatThrownBy(() -> commandService.updateProduct(command))
                    .isInstanceOf(ProductNotFoundException.class);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("扣减库存后应使缓存失效")
    void decrementStock_shouldEvictCache() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

            commandService.decrementStock(new DecrementStockCommand(1L, 1));

            verify(productCachePort).evictProductCache(1L);
            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("标记售出后应使缓存失效")
    void markAsSold_shouldEvictCache() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

            commandService.markAsSold(new MarkAsSoldCommand(1L));

            verify(productCachePort).evictProductCache(1L);
            verify(domainEventPublisher).publish(any(BaseDomainEvent.class));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
