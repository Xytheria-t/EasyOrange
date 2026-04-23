package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order Saga 集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Order Saga 集成测试")
class CreateOrderSagaIntegrationTest {

    @Autowired
    private CreateOrderSaga createOrderSaga;

    @Test
    @DisplayName("订单创建 Saga 成功流程")
    void testExecute_Success() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
                .productId(1L)
                .address("测试地址")
                .phone("12345678901")
                .remark("测试订单")
                .paymentMethod(1)
                .build();

        // When
        CreateOrderResult result = createOrderSaga.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isNotNull();
        assertThat(result.orderNo()).isNotNull();
        assertThat(result.orderNo()).startsWith("ORD");
    }

    @Test
    @DisplayName("订单创建 Saga - 商品不存在时失败并补偿")
    void testExecute_ProductNotFound_ShouldCompensate() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
                .productId(999999L) // 不存在的商品 ID
                .address("测试地址")
                .phone("12345678901")
                .remark("测试订单")
                .paymentMethod(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("订单创建 Saga - 购买自己商品时失败")
    void testExecute_BuyOwnProduct_ShouldFail() {
        // Given - 假设当前登录用户是商品所有者
        // 这个测试需要 mock 用户上下文，实际测试中可能需要调整
        CreateOrderCommand command = CreateOrderCommand.builder()
                .productId(1L)
                .address("测试地址")
                .phone("12345678901")
                .remark("测试订单")
                .paymentMethod(1)
                .build();

        // 这个测试的结果取决于当前登录用户和商品所有者的关系
        // 如果用户 1 是商品 1 的所有者，应该抛出异常
        assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("不能购买自己的商品");
    }

    @Test
    @DisplayName("订单创建 Saga - 商品已下架时失败")
    void testExecute_ProductOffline_ShouldFail() {
        // Given - 假设商品 2 已下架
        CreateOrderCommand command = CreateOrderCommand.builder()
                .productId(2L) // 假设商品 2 状态为 offline
                .address("测试地址")
                .phone("12345678901")
                .remark("测试订单")
                .paymentMethod(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("商品已下架");
    }

    @Test
    @DisplayName("订单创建 Saga - 库存不足时失败")
    void testExecute_InsufficientStock_ShouldFail() {
        // Given - 假设商品 3 库存为 0
        CreateOrderCommand command = CreateOrderCommand.builder()
                .productId(3L) // 假设商品 3 库存不足
                .address("测试地址")
                .phone("12345678901")
                .remark("测试订单")
                .paymentMethod(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("商品库存不足");
    }
}
