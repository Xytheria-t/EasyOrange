package com.cartethyia.easyorange.adapter.event;

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
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private OrderReadRepository orderReadRepository;

    private OrderNotificationEventConsumer consumer;

    private OrderReadModel orderReadModel;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new OrderNotificationEventConsumer(idempotencyChecker, metricsService,
                messageCommandHandler, orderReadRepository);
        orderReadModel = new OrderReadModel(
                ORDER_ID, "ORD" + ORDER_ID,
                BUYER_ID, SELLER_ID, List.of(),
                BigDecimal.valueOf(99.99), OrderStatus.PENDING_PAYMENT.getCode(), "待付款",
                PaymentStatus.UNPAID.getCode(),
                "地址", "13800138000", "备注",
                null, null, null, null
        );
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    private void mockLockSuccess() {
        when(idempotencyChecker.isDuplicate(anyString(), anyString())).thenReturn(false);
        when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
    }

    private void mockFindOrderReadModel() {
        when(orderReadRepository.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(orderReadModel));
    }

    @Nested
    @DisplayName("onOrderCreated")
    class OnOrderCreatedTests {

        @Test
        @DisplayName("订单创建后发送站内信通知")
        void onOrderCreated_shouldSendNotification() {
            mockLockSuccess();

            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderCreated"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderCreated"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已创建");
            assertThat(command.getContent()).isEqualTo("您的订单已创建，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("onOrderPaid")
    class OnOrderPaidTests {

        @Test
        @DisplayName("订单支付后发送站内信通知")
        void onOrderPaid_shouldSendNotification() {
            mockLockSuccess();
            mockFindOrderReadModel();

            OrderPaidEvent event = new OrderPaidEvent(ORDER_ID, "1");

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderPaid"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderPaid"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已支付");
            assertThat(command.getContent()).isEqualTo("您的订单已支付成功，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("onOrderShipped")
    class OnOrderShippedTests {

        @Test
        @DisplayName("订单发货后发送站内信通知")
        void onOrderShipped_shouldSendNotification() {
            mockLockSuccess();
            mockFindOrderReadModel();

            OrderShippedEvent event = new OrderShippedEvent(ORDER_ID);

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderShipped"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderShipped"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已发货");
            assertThat(command.getContent()).isEqualTo("您的订单已发货，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("onOrderCompleted")
    class OnOrderCompletedTests {

        @Test
        @DisplayName("订单完成后发送站内信通知")
        void onOrderCompleted_shouldSendNotification() {
            mockLockSuccess();
            mockFindOrderReadModel();

            OrderCompletedEvent event = new OrderCompletedEvent(ORDER_ID, List.of(PRODUCT_ID));

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderCompleted"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderCompleted"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已完成");
            assertThat(command.getContent()).isEqualTo("您的订单已完成，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("onOrderCancelled")
    class OnOrderCancelledTests {

        @Test
        @DisplayName("订单取消后发送站内信通知")
        void onOrderCancelled_shouldSendNotification() {
            mockLockSuccess();
            mockFindOrderReadModel();

            OrderCancelledEvent event = new OrderCancelledEvent(ORDER_ID, List.of(PRODUCT_ID), "取消原因");

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderCancelled"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderCancelled"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已取消");
            assertThat(command.getContent()).isEqualTo("您的订单已取消，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("onOrderRefunded")
    class OnOrderRefundedTests {

        @Test
        @DisplayName("订单退款后发送站内信通知")
        void onOrderRefunded_shouldSendNotification() {
            mockLockSuccess();
            mockFindOrderReadModel();

            OrderRefundedEvent event = new OrderRefundedEvent(ORDER_ID, List.of(PRODUCT_ID), "退款原因");

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker).isDuplicate(
                    eq(CONSUMER_ID + ":OrderRefunded"), anyString());
            verify(idempotencyChecker).tryMark(
                    eq(CONSUMER_ID + ":OrderRefunded"), anyString());

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertThat(command.getReceiverId()).isEqualTo(BUYER_ID);
            assertThat(command.getTitle()).isEqualTo("订单已退款");
            assertThat(command.getContent()).isEqualTo("您的订单已退款，订单号: " + ORDER_ID);
            assertThat(command.getBusinessId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("幂等性处理")
    class IdempotencyTests {

        @Test
        @DisplayName("重复事件不发送通知")
        void onEvent_withDuplicateEvent_shouldSkip() {
            when(idempotencyChecker.isDuplicate(anyString(), anyString())).thenReturn(true);

            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.handle(event, buildMessage());

            verify(idempotencyChecker, never()).tryMark(anyString(), anyString());
            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }

        @Test
        @DisplayName("锁竞争失败时不发送通知")
        void onEvent_whenLockFails_shouldSkip() {
            when(idempotencyChecker.isDuplicate(anyString(), anyString())).thenReturn(false);
            when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(false);

            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.handle(event, buildMessage());

            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }
    }

    @Nested
    @DisplayName("订单不存在处理")
    class OrderNotFoundTests {

        @Test
        @DisplayName("订单不存在时跳过通知")
        void onEvent_whenOrderNotFound_shouldSkip() {
            mockLockSuccess();
            when(orderReadRepository.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

            OrderPaidEvent event = new OrderPaidEvent(ORDER_ID, "1");

            consumer.handle(event, buildMessage());

            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }
    }
}
