package com.cartethyia.easyorange.product.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductTestFixture;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品命令处理器测试")
class ProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private ProductCommandHandler commandHandler;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        commandHandler = new ProductCommandHandler(productRepository, domainEventPublisher);
        existingProduct = ProductTestFixture.onlineProduct();
    }

    @Test
    @DisplayName("创建商品应调用仓储保存")
    void createProduct_shouldSaveToRepository() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                return p.assignId("42");
            });

            CreateProductCommand command = new CreateProductCommand(
                    "2",
                    "测试商品",
                    new BigDecimal("100"),
                    null,
                    10,
                    "1",
                    "北京",
                    "微信",
                    "描述",
                    java.util.List.of("http://img/1.jpg"));

            String productId = commandHandler.createProduct(command);

            assertThat(productId).isEqualTo("42");
            verify(productRepository).save(any(Product.class));
            verify(domainEventPublisher).publish(any(ProductCreatedEvent.class));
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

            UpdateProductCommand command =
                    new UpdateProductCommand("999", null, "新名称", null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> commandHandler.updateProduct(command))
                    .isInstanceOf(ProductNotFoundException.class);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
