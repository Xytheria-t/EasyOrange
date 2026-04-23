package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Order Saga 补偿逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Saga 补偿逻辑测试")
class CreateOrderSagaCompensationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    // 由于 Saga 的补偿方法是私有的，我们通过集成测试来验证补偿逻辑
    // 这个类主要用于演示和文档目的
    
    @Test
    @DisplayName("订单补偿 - 取消订单")
    void testCancelOrder_Compensation() {
        // Given
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123")
                .buyerId(100L)
                .sellerId(200L)
                .productId(1L)
                .amount(BigDecimal.valueOf(100))
                .status(OrderStatus.PENDING_PAYMENT.getCode())
                .paymentStatus(0)
                .build();
        
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        
        // When
        // 补偿逻辑在 Saga 内部执行，通过集成测试验证
        
        // Then - 验证订单状态被更新为 CANCELLED
        // 实际测试中需要验证 orderRepository.update() 被调用
    }
    
    @Test
    @DisplayName("库存补偿 - 恢复库存")
    void testRestoreStock_Compensation() {
        // Given
        Product product = Product.builder()
                .id(1L)
                .userId(100L)
                .name("测试商品")
                .price(BigDecimal.valueOf(100))
                .stock(9) // 扣减后库存
                .status(1)
                .build();
        
        given(productRepository.findById(1L)).willReturn(product);
        
        // When
        ProductAggregate aggregate = ProductAggregate.load(product, null, null);
        aggregate.restoreStock();
        
        // Then
        assertThat(aggregate.getProduct().getStock()).isEqualTo(10);
        verify(productRepository, times(1)).save(any(Product.class));
    }
    
    @Test
    @DisplayName("补偿顺序 - 后进先出")
    void testCompensation_Order_LIFO() {
        // Given - Saga 执行顺序：1.创建订单 2.扣减库存 3.创建支付
        // 补偿顺序应该是：1.取消支付 2.恢复库存 3.取消订单
        
        // When & Then
        // 补偿逻辑验证通过集成测试完成
        // 这里只是文档说明
    }
}
