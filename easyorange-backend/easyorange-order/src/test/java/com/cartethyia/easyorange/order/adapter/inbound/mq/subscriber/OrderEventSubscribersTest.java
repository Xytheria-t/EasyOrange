package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumers 单元测试")
class OrderEventSubscribersTest {

    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    @Nested
    @DisplayName("OrderCreatedEventConsumer")
    class OrderCreatedEventConsumerTests {

        @Mock
        private DomainEventPublisher domainEventPublisher;

        private OrderCreatedEventConsumer consumer;

        @Captor
        private ArgumentCaptor<StockReservationRequestedEvent> stockEventCaptor;

        @BeforeEach
        void setUp() {
            consumer = new OrderCreatedEventConsumer(domainEventPublisher);
        }

        @Test
        @DisplayName("收到订单创建事件后发布库存预留请求")
        void onOrderCreated_shouldPublishStockReservationRequest() {
            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.onOrderCreated(event);

            verify(domainEventPublisher).publish(stockEventCaptor.capture());
            StockReservationRequestedEvent captured = stockEventCaptor.getValue();
            assertThat(captured.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(captured.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(captured.getQuantity()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("OrderCancelledEventConsumer")
    class OrderCancelledEventConsumerTests {

        @Mock
        private ProductInventoryPort productInventoryPort;

        private OrderCancelledEventConsumer consumer;

        @BeforeEach
        void setUp() {
            consumer = new OrderCancelledEventConsumer(productInventoryPort);
        }

        @Test
        @DisplayName("收到订单取消事件后恢复库存")
        void onOrderCancelled_shouldRestoreStock() {
            OrderCancelledEvent event = new OrderCancelledEvent(ORDER_ID, List.of(PRODUCT_ID), "取消原因");

            consumer.onOrderCancelled(event);

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("OrderCompletedEventConsumer")
    class OrderCompletedEventConsumerTests {

        @Mock
        private ProductInventoryPort productInventoryPort;

        private OrderCompletedEventConsumer consumer;

        @BeforeEach
        void setUp() {
            consumer = new OrderCompletedEventConsumer(productInventoryPort);
        }

        @Test
        @DisplayName("收到订单完成事件后标记商品已售")
        void onOrderCompleted_shouldMarkAsSold() {
            OrderCompletedEvent event = new OrderCompletedEvent(ORDER_ID, List.of(PRODUCT_ID));

            consumer.onOrderCompleted(event);

            verify(productInventoryPort).markAsSold(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("OrderRefundedEventConsumer")
    class OrderRefundedEventConsumerTests {

        @Mock
        private ProductInventoryPort productInventoryPort;

        private OrderRefundedEventConsumer consumer;

        @BeforeEach
        void setUp() {
            consumer = new OrderRefundedEventConsumer(productInventoryPort);
        }

        @Test
        @DisplayName("收到订单退款事件后恢复库存")
        void onOrderRefunded_shouldRestoreStock() {
            OrderRefundedEvent event = new OrderRefundedEvent(ORDER_ID, List.of(PRODUCT_ID), "退款原因");

            consumer.onOrderRefunded(event);

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }
    }
}