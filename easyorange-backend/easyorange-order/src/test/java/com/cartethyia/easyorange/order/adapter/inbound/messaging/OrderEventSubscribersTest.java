package com.cartethyia.easyorange.order.adapter.inbound.messaging;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderLifecycleEventConsumer 单元测试")
class OrderEventSubscribersTest {

    private static final String ORDER_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @Nested
    @DisplayName("OrderLifecycleEventConsumer")
    class OrderLifecycleEventConsumerTests {

        @Mock
        private ProductInventoryPort productInventoryPort;

        @Mock
        private PaymentGatewayPort paymentGatewayPort;

        @Mock
        private EventIdempotencyChecker idempotencyChecker;

        private OrderLifecycleEventConsumer consumer;

        @BeforeEach
        void setUp() {
            // Allow idempotency claim to pass through (fail-open: returns true when Redis unavailable)
            lenient().when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
            var metricsService = new EventMetricsService(new SimpleMeterRegistry());
            consumer = new OrderLifecycleEventConsumer(
                    idempotencyChecker, metricsService, productInventoryPort, paymentGatewayPort);
        }

        private Message buildMessage() {
            var props = new MessageProperties();
            props.setMessageId(java.util.UUID.randomUUID().toString());
            return new Message(new byte[0], props);
        }

        @Test
        @DisplayName("收到订单创建事件后不再异步预留库存（已由 OrderCommandHandler 同步扣减）")
        void onOrderCreated_shouldNotReserveStock() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "evt-1",
                    ORDER_ID,
                    BUYER_ID,
                    SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(
                            PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.onOrderCreated(event, buildMessage());

            verify(productInventoryPort, never()).restoreStock(anyString());
            verify(productInventoryPort, never()).markAsSold(anyString());
        }

        @Test
        @DisplayName("收到订单取消事件后恢复库存")
        void onOrderCancelled_shouldRestoreStock() {
            OrderCancelledEvent event =
                    new OrderCancelledEvent("evt-2", ORDER_ID, BUYER_ID, List.of(PRODUCT_ID), "取消原因");

            consumer.onOrderCancelled(event, buildMessage());

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单完成事件后标记商品已售")
        void onOrderCompleted_shouldMarkAsSold() {
            OrderCompletedEvent event =
                    new OrderCompletedEvent("evt-3", ORDER_ID, BUYER_ID, SELLER_ID, List.of(PRODUCT_ID));

            consumer.onOrderCompleted(event, buildMessage());

            verify(productInventoryPort).markAsSold(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单退款事件后恢复库存并触发支付退款")
        void onOrderRefunded_shouldRestoreStockAndRefund() {
            OrderRefundedEvent event = new OrderRefundedEvent("evt-4", ORDER_ID, BUYER_ID, List.of(PRODUCT_ID), "退款原因");

            consumer.onOrderRefunded(event, buildMessage());

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
            verify(paymentGatewayPort).refundPayment(ORDER_ID, "退款原因");
        }
    }
}
