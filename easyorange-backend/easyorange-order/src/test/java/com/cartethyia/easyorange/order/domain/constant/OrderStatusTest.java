package com.cartethyia.easyorange.order.domain.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
