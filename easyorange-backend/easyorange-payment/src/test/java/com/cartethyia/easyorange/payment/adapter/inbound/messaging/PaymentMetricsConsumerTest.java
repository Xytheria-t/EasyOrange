package com.cartethyia.easyorange.payment.adapter.inbound.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMetricsConsumer 测试")
class PaymentMetricsConsumerTest {

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    private SimpleMeterRegistry meterRegistry;
    private PaymentMetricsConsumer consumer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        EventMetricsService eventMetricsService = new EventMetricsService(meterRegistry);
        PaymentMetricsService metricsService = new PaymentMetricsService(meterRegistry);
        consumer = new PaymentMetricsConsumer(idempotencyChecker, eventMetricsService, metricsService);
    }

    private Message message() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("evt-1");
        return new Message("{}".getBytes(StandardCharsets.UTF_8), props);
    }

    private Counter counter(String name, String type) {
        return meterRegistry.counter(name, "type", type);
    }

    @Test
    @DisplayName("支付创建事件累加创建指标")
    void onPaymentCreated_recordsCreated() {
        consumer.onPaymentCreated(
                new PaymentCreatedEvent("evt-1", "1001", "PAY123", "2001", "3001", new BigDecimal("100.00"), "WECHAT"),
                message());

        assertThat(counter("payment.created.total", "payment").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("支付成功事件累加成功指标")
    void onPaymentSucceeded_recordsSuccess() {
        consumer.onPaymentSucceeded(new PaymentSucceededEvent("evt-2", "1001", "2001", "TXN_1"), message());

        assertThat(counter("payment.success.total", "payment").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("支付失败事件累加失败指标")
    void onPaymentFailed_recordsFailed() {
        consumer.onPaymentFailed(new PaymentFailedEvent("evt-3", "1001", "失败"), message());

        assertThat(counter("payment.failed.total", "payment").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("退款事件累加退款指标")
    void onPaymentRefunded_recordsRefund() {
        consumer.onPaymentRefunded(new PaymentRefundedEvent("evt-4", "1001", "用户退款"), message());

        assertThat(counter("payment.refund.total", "refund").count()).isEqualTo(1);
    }
}
