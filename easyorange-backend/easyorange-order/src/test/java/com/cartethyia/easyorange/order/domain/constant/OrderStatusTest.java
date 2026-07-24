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
        "0, PENDING_PAYMENT, 待付款",
        "1, PAID, 已付款",
        "2, SHIPPED, 已发货",
        "3, COMPLETED, 已完成",
        "4, CANCELLED, 已取消",
        "5, REFUNDED, 已退款"
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
        assertThatThrownBy(() -> OrderStatus.fromCode("-1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderStatus.fromCode("99"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderStatus.fromCode(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 待付款",
        "1, 已付款",
        "2, 已发货",
        "3, 已完成",
        "4, 已取消",
        "5, 已退款"
    })
    void getDescByCode_withAllCodes_shouldReturnNonEmpty(String code, String expectedDesc) {
        String desc = OrderStatus.getDescByCode(code);

        assertThat(desc).isNotNull().isNotEmpty().isEqualTo(expectedDesc);
    }

    @Test
    void getDescByCode_withInvalidCode_shouldReturnUnknown() {
        assertThat(OrderStatus.getDescByCode("-1")).isEqualTo("未知状态");
        assertThat(OrderStatus.getDescByCode("99")).isEqualTo("未知状态");
        assertThat(OrderStatus.getDescByCode(null)).isEqualTo("未知状态");
    }
}
