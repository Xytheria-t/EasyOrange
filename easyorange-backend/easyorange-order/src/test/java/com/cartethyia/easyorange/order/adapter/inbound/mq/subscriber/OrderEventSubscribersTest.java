package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaEventConsumer 单元测试")
class OrderEventSubscribersTest {

    private static final String ORDER_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @Nested
    @DisplayName("OrderSagaEventConsumer")
    class OrderSagaEventConsumerTests {

        @Mock
        private DomainEventPublisher domainEventPublisher;

        @Mock
        private ProductInventoryPort productInventoryPort;

        @Mock
        private EventIdempotencyChecker idempotencyChecker;

        private OrderSagaEventConsumer consumer;

        @Captor
        private ArgumentCaptor<StockReservationRequestedEvent> stockEventCaptor;

        @BeforeEach
        void setUp() {
            // Allow idempotency check to pass through (fail-open: returns true when Redis unavailable)
            lenient().when(idempotencyChecker.isDuplicate(anyString(), anyString())).thenReturn(false);
            lenient().when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
            var metricsService = new EventMetricsService(new SimpleMeterRegistry());
            consumer = new OrderSagaEventConsumer(idempotencyChecker, metricsService,
                    domainEventPublisher, productInventoryPort);
        }

        private Message buildMessage() {
            var props = new MessageProperties();
            props.setMessageId(java.util.UUID.randomUUID().toString());
            return new Message(new byte[0], props);
        }

        @Test
        @DisplayName("收到订单创建事件后发布库存预留请求")
        void onOrderCreated_shouldPublishStockReservationRequest() {
            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.handle(event, buildMessage());

            verify(domainEventPublisher).publish(stockEventCaptor.capture());
            StockReservationRequestedEvent captured = stockEventCaptor.getValue();
            assertThat(captured.orderId()).isEqualTo(ORDER_ID);
            assertThat(captured.productId()).isEqualTo(PRODUCT_ID);
            assertThat(captured.quantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("收到订单取消事件后恢复库存")
        void onOrderCancelled_shouldRestoreStock() {
            OrderCancelledEvent event = new OrderCancelledEvent(ORDER_ID, List.of(PRODUCT_ID), "取消原因");

            consumer.handle(event, buildMessage());

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单完成事件后标记商品已售")
        void onOrderCompleted_shouldMarkAsSold() {
            OrderCompletedEvent event = new OrderCompletedEvent(ORDER_ID, List.of(PRODUCT_ID));

            consumer.handle(event, buildMessage());

            verify(productInventoryPort).markAsSold(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单退款事件后恢复库存")
        void onOrderRefunded_shouldRestoreStock() {
            OrderRefundedEvent event = new OrderRefundedEvent(ORDER_ID, List.of(PRODUCT_ID), "退款原因");

            consumer.handle(event, buildMessage());

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }
    }
}
