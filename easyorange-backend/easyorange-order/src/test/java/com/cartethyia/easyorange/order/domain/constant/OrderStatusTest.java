package com.cartethyia.easyorange.order.domain.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderStatus 枚举单元测试")
class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
        "PENDING_PAYMENT, PENDING_PAYMENT, 待付款",
        "PAID, PAID, 已付款",
        "SHIPPED, SHIPPED, 已发货",
        "COMPLETED, COMPLETED, 已完成",
        "CANCELLED, CANCELLED, 已取消",
        "REFUNDED, REFUNDED, 已退款"
    })
    void fromCode_withAllValidCodes_shouldReturnCorrectEnum(String code, String expectedName, String expectedDesc) {
        OrderStatus status = OrderStatus.fromCode(code);

        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo(expectedName);
        assertThat(status.getCode()).isEqualTo(code);
        assertThat(status.getDesc()).isEqualTo(expectedDesc);
    }

    @Test
    void fromCode_withInvalidCode_shouldThrow() {
        assertThatThrownBy(() -> OrderStatus.fromCode("UNKNOWN"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderStatus.fromCode("0"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderStatus.fromCode(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("canTransitionTo 派生全矩阵与状态机定义一致")
    void canTransitionTo_fullMatrixMatchesStateMachine() {
        // 预期矩阵由 OrderAction 的前置/目标状态派生而来，此测试守护派生逻辑不被意外改动。
        Map<OrderStatus, Set<OrderStatus>> expected = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID,           Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED,        Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED),
            OrderStatus.COMPLETED,      Set.of(),
            OrderStatus.CANCELLED,      Set.of(),
            OrderStatus.REFUNDED,       Set.of()
        );

        for (OrderStatus from : OrderStatus.values()) {
            for (OrderStatus to : OrderStatus.values()) {
                boolean expectedAllowed = expected.getOrDefault(from, Set.of()).contains(to);
                assertThat(from.canTransitionTo(to))
                        .as("%s → %s", from, to)
                        .isEqualTo(expectedAllowed);
            }
        }
    }
}
