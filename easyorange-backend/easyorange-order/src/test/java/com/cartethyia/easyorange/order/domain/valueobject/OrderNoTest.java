package com.cartethyia.easyorange.order.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrderNo 值对象测试")
class OrderNoTest {

    private static final String ORDER_ID = "018f7c1d-0000-7000-8000-000000000001";

    @Test
    @DisplayName("由订单 ID 派生：ORD 前缀 + 订单 ID")
    void forOrderId_prefixesOrderId() {
        assertThat(OrderNo.forOrderId(ORDER_ID).value()).isEqualTo("ORD" + ORDER_ID);
    }

    @Test
    @DisplayName("订单 ID 为空/空白时拒绝派生")
    void forOrderId_blankOrderId_throws() {
        assertThatThrownBy(() -> OrderNo.forOrderId("  ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> OrderNo.forOrderId(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("前缀不符的持久化值被拒绝")
    void of_wrongPrefix_throws() {
        assertThatThrownBy(() -> OrderNo.of("018f7c1d-0000-7000-8000-000000000001"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("重建合法持久化值")
    void of_validValue_rebuilds() {
        assertThat(OrderNo.of("ORD123").value()).isEqualTo("ORD123");
    }
}
