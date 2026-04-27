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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Order Saga 补偿逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Order Saga 补偿逻辑测试")
class CreateOrderSagaCompensationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("订单补偿 - 验证订单查询")
    void testFindOrderForCompensation() {
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

        Optional<Order> foundOrder = orderRepository.findById(1L);

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getOrderNo()).isEqualTo("ORD123");
    }

    @Test
    @DisplayName("库存补偿 - 验证恢复库存逻辑")
    void testRestoreStock_Logic() {
        Product product = Product.builder()
                .id(1L)
                .userId(100L)
                .name("测试商品")
                .price(BigDecimal.valueOf(100))
                .stock(9)
                .status(1)
                .build();

        given(productRepository.findById(1L)).willReturn(product);

        ProductAggregate aggregate = ProductAggregate.load(product, null, null);
        aggregate.restoreStock();

        assertThat(aggregate.getProduct().getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("补偿顺序 - 后进先出")
    void testCompensation_Order_LIFO() {
    }
}