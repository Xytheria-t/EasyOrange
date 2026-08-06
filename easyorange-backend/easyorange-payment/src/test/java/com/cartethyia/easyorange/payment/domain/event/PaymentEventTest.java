package com.cartethyia.easyorange.payment.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("支付领域事件测试")
class PaymentEventTest {

    @Test
    @DisplayName("创建事件关联聚合根 ID")
    void createdEvent_aggregateId() {
        PaymentCreatedEvent event =
                new PaymentCreatedEvent("1001", "PAY123", "2001", "3001", new BigDecimal("100.00"), "WECHAT");

        assertThat(event.aggregateId()).isEqualTo("1001");
        assertThat(event.eventType()).isNotBlank();
    }

    @Test
    @DisplayName("成功事件关联聚合根 ID")
    void succeededEvent_aggregateId() {
        assertThat(new PaymentSucceededEvent("1001", "TXN_1").aggregateId()).isEqualTo("1001");
    }

    @Test
    @DisplayName("失败事件关联聚合根 ID")
    void failedEvent_aggregateId() {
        assertThat(new PaymentFailedEvent("1001", "原因").aggregateId()).isEqualTo("1001");
    }

    @Test
    @DisplayName("退款事件关联聚合根 ID")
    void refundedEvent_aggregateId() {
        assertThat(new PaymentRefundedEvent("1001", "用户退款").aggregateId()).isEqualTo("1001");
    }

    @Test
    @DisplayName("关闭事件关联聚合根 ID")
    void closedEvent_aggregateId() {
        assertThat(new PaymentClosedEvent("1001").aggregateId()).isEqualTo("1001");
    }
}
