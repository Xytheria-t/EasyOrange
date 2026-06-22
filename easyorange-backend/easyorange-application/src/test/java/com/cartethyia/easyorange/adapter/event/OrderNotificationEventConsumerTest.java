package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderNotificationEventConsumer 单元测试")
class OrderNotificationEventConsumerTest {

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private MessageCommandHandler messageCommandHandler;

    @Mock
    private OrderReadRepository orderReadRepository;

    @InjectMocks
    private OrderNotificationEventConsumer consumer;

    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    private OrderReadModel orderReadModel;

    @BeforeEach
    void setUp() {
        orderReadModel = new OrderReadModel(
                ORDER_ID, "ORD" + ORDER_ID,
                BUYER_ID, SELLER_ID, List.of(),
                BigDecimal.valueOf(99.99), 0, "待付款", 0,
                "地址", "13800138000", "备注",
                null, null, null, null
        );
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

            consumer.onOrderCreated(event);

            verify(idempotencyChecker).isDuplicate("OrderCreated", "created:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderCreated", "created:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已创建", command.getTitle());
            assertEquals("您的订单已创建，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            OrderPaidEvent event = new OrderPaidEvent(ORDER_ID, 1);

            consumer.onOrderPaid(event);

            verify(idempotencyChecker).isDuplicate("OrderPaid", "paid:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderPaid", "paid:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已支付", command.getTitle());
            assertEquals("您的订单已支付成功，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            consumer.onOrderShipped(event);

            verify(idempotencyChecker).isDuplicate("OrderShipped", "shipped:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderShipped", "shipped:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已发货", command.getTitle());
            assertEquals("您的订单已发货，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            consumer.onOrderCompleted(event);

            verify(idempotencyChecker).isDuplicate("OrderCompleted", "completed:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderCompleted", "completed:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已完成", command.getTitle());
            assertEquals("您的订单已完成，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            consumer.onOrderCancelled(event);

            verify(idempotencyChecker).isDuplicate("OrderCancelled", "cancelled:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderCancelled", "cancelled:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已取消", command.getTitle());
            assertEquals("您的订单已取消，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            consumer.onOrderRefunded(event);

            verify(idempotencyChecker).isDuplicate("OrderRefunded", "refunded:" + ORDER_ID);
            verify(idempotencyChecker).tryMark("OrderRefunded", "refunded:" + ORDER_ID);

            var captor = ArgumentCaptor.forClass(SendSystemMessageCommand.class);
            verify(messageCommandHandler).handle(captor.capture());
            var command = captor.getValue();
            assertEquals(BUYER_ID, command.getReceiverId());
            assertEquals("订单已退款", command.getTitle());
            assertEquals("您的订单已退款，订单号: " + ORDER_ID, command.getContent());
            assertEquals(ORDER_ID, command.getBusinessId());
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

            consumer.onOrderCreated(event);

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

            consumer.onOrderCreated(event);

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

            OrderPaidEvent event = new OrderPaidEvent(ORDER_ID, 1);

            consumer.onOrderPaid(event);

            verify(messageCommandHandler, never()).handle(any(SendSystemMessageCommand.class));
        }
    }
}
