package com.cartethyia.easyorange.order.adapter.inbound.messaging;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSucceededEventConsumer 单元测试")
class PaymentSucceededEventConsumerTest {

    private static final String ORDER_ID = "100";
    private static final String PAYMENT_ID = "3001";

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private OrderCommandHandler orderCommandHandler;

    private PaymentSucceededEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // Allow idempotency claim to pass through (fail-open: returns true when Redis unavailable)
        lenient().when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new PaymentSucceededEventConsumer(idempotencyChecker, metricsService, orderCommandHandler);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("收到支付成功事件后委托订单命令处理器置 PAID")
    void onPaymentSucceeded_delegatesToCommandHandler() {
        var event = new PaymentSucceededEvent("evt-1", PAYMENT_ID, ORDER_ID, "TXN_001");

        consumer.onPaymentSucceeded(event, buildMessage());

        verify(orderCommandHandler).handlePaymentSucceeded(ORDER_ID);
    }

    @Test
    @DisplayName("旧版事件缺少 orderId 时降级跳过并告警")
    void onPaymentSucceeded_missingOrderId_skips() {
        var event = new PaymentSucceededEvent("evt-1", PAYMENT_ID, null, "TXN_001");

        consumer.onPaymentSucceeded(event, buildMessage());

        verify(orderCommandHandler, never()).handlePaymentSucceeded(anyString());
    }
}
