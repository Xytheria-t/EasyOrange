package com.cartethyia.easyorange.adapter.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderNotificationEventConsumer 单元测试")
class OrderNotificationEventConsumerTest {

    private static final String ORDER_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String CONSUMER_ID = "OrderNotificationEventConsumer";

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private MessageCommandHandler messageCommandHandler;

    private OrderNotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new OrderNotificationEventConsumer(idempotencyChecker, metricsService, messageCommandHandler);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    private void mockClaimSuccess() {
        when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
    }

    /** 事件载荷自包含 buyerId，通知无需回查订单 — 断言通知内容即验证全部行为。 */
    private void verifyNotificationSent(String eventType, String title, String content) {
        verify(idempotencyChecker).tryMark(eq(CONSUMER_ID + ":" + eventType), anyString());

        var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
        verify(messageCommandHandler).handle(captor.capture());
        var command = captor.getValue();
        assertThat(command.receiverId()).isEqualTo(BUYER_ID);
        assertThat(command.title()).isEqualTo(title);
        assertThat(command.content()).isEqualTo(content);
        assertThat(command.businessId()).isEqualTo(ORDER_ID);
    }

    @Nested
    @DisplayName("订单事件 → 站内信通知")
    class NotificationTests {

        @Test
        @DisplayName("订单创建后通知买家")
        void onOrderCreated_shouldSendNotification() {
            mockClaimSuccess();

            OrderCreatedEvent event = new OrderCreatedEvent(
                    "evt-1",
                    ORDER_ID,
                    BUYER_ID,
                    SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(
                            PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.onOrderEvent(event, buildMessage());

            verifyNotificationSent("OrderCreated", "订单已创建", "您的订单已创建，订单号: " + ORDER_ID);
        }

        @Test
        @DisplayName("订单支付后通知买家")
        void onOrderPaid_shouldSendNotification() {
            mockClaimSuccess();

            consumer.onOrderEvent(new OrderPaidEvent("evt-2", ORDER_ID, BUYER_ID, "1"), buildMessage());

            verifyNotificationSent("OrderPaid", "订单已支付", "您的订单已支付成功，订单号: " + ORDER_ID);
        }

        @Test
        @DisplayName("订单发货后通知买家")
        void onOrderShipped_shouldSendNotification() {
            mockClaimSuccess();

            consumer.onOrderEvent(new OrderShippedEvent("evt-3", ORDER_ID, BUYER_ID), buildMessage());

            verifyNotificationSent("OrderShipped", "订单已发货", "您的订单已发货，订单号: " + ORDER_ID);
        }

        @Test
        @DisplayName("订单完成后通知买家")
        void onOrderCompleted_shouldSendNotification() {
            mockClaimSuccess();

            consumer.onOrderEvent(
                    new OrderCompletedEvent("evt-4", ORDER_ID, BUYER_ID, SELLER_ID, List.of(PRODUCT_ID)),
                    buildMessage());

            verifyNotificationSent("OrderCompleted", "订单已完成", "您的订单已完成，订单号: " + ORDER_ID);
        }

        @Test
        @DisplayName("订单取消后通知买家")
        void onOrderCancelled_shouldSendNotification() {
            mockClaimSuccess();

            consumer.onOrderEvent(
                    new OrderCancelledEvent("evt-5", ORDER_ID, BUYER_ID, List.of(PRODUCT_ID), "取消原因"), buildMessage());

            verifyNotificationSent("OrderCancelled", "订单已取消", "您的订单已取消，订单号: " + ORDER_ID);
        }

        @Test
        @DisplayName("订单退款后通知买家")
        void onOrderRefunded_shouldSendNotification() {
            mockClaimSuccess();

            consumer.onOrderEvent(
                    new OrderRefundedEvent("evt-6", ORDER_ID, BUYER_ID, List.of(PRODUCT_ID), "退款原因"), buildMessage());

            verifyNotificationSent("OrderRefunded", "订单已退款", "您的订单已退款，订单号: " + ORDER_ID);
        }
    }

    @Nested
    @DisplayName("旧版消息兼容（滚动部署窗口）")
    class LegacyPayloadTests {

        @Test
        @DisplayName("旧版消息（无 buyerId）降级跳过，不发送通知")
        void onEvent_withoutBuyerId_shouldSkip() {
            mockClaimSuccess();

            consumer.onOrderEvent(new OrderPaidEvent("evt-8", ORDER_ID, null, "1"), buildMessage());

            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }

        @Test
        @DisplayName("旧版创建消息（无 buyerId）同样降级跳过，不落脏数据")
        void onOrderCreated_withoutBuyerId_shouldSkip() {
            mockClaimSuccess();

            consumer.onOrderEvent(
                    new OrderCreatedEvent(
                            "evt-9",
                            ORDER_ID,
                            null,
                            SELLER_ID,
                            List.of(new OrderCreatedEvent.OrderItemPayload(
                                    PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                            BigDecimal.valueOf(99.99)),
                    buildMessage());

            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }
    }

    @Nested
    @DisplayName("幂等性处理")
    class IdempotencyTests {

        @Test
        @DisplayName("重复事件（领取处理权失败）不发送通知")
        void onEvent_withDuplicateEvent_shouldSkip() {
            when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(false);

            consumer.onOrderEvent(new OrderPaidEvent("evt-7", ORDER_ID, BUYER_ID, "1"), buildMessage());

            verify(idempotencyChecker).tryMark(anyString(), anyString());
            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }
    }
}
