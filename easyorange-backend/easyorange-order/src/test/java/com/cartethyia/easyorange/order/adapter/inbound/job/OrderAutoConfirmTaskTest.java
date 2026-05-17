package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.port.output.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.output.OrderRepository;
import com.cartethyia.easyorange.order.infrastructure.config.OrderTimeoutProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAutoConfirmTask 单元测试")
class OrderAutoConfirmTaskTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrderTimeoutProperties properties;

    @Mock
    private OrderCachePort orderCachePort;

    @InjectMocks
    private OrderAutoConfirmTask orderAutoConfirmTask;

    private static final Long ORDER_ID_1 = 100L;
    private static final Long ORDER_ID_2 = 101L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    private OrderAggregate shippedOrder1;
    private OrderAggregate shippedOrder2;

    @BeforeEach
    void setUp() {
        shippedOrder1 = buildShippedOrder(ORDER_ID_1);
        shippedOrder2 = buildShippedOrder(ORDER_ID_2);
    }

    private OrderAggregate buildShippedOrder(Long orderId) {
        return OrderAggregate.fromRaw(
                orderId, "ORD" + orderId,
                BUYER_ID, SELLER_ID, 10L,
                BigDecimal.valueOf(99.99),
                OrderStatus.SHIPPED.getCode(), 1,
                "地址", "13800138000", "备注",
                null, null
        );
    }

    @Nested
    @DisplayName("autoConfirmReceipt()")
    class AutoConfirmReceiptTests {

        @Test
        @DisplayName("自动确认所有已发货订单")
        void autoConfirmReceipt_shouldConfirmAll() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(shippedOrder1, shippedOrder2));

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, times(2)).update(any(OrderAggregate.class));
            verify(domainEventPublisher, times(2)).publish(any(OrderCompletedEvent.class));
            verify(orderCachePort, times(2)).evictOrderCache(anyLong(), anyLong());
        }

        @Test
        @DisplayName("没有已发货订单时不执行任何操作")
        void autoConfirmReceipt_withNoShippedOrders_shouldDoNothing() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
            verify(orderCachePort, never()).evictOrderCache(anyLong(), anyLong());
        }

        @Test
        @DisplayName("处理异常时优雅跳过，不影响其他订单")
        void autoConfirmReceipt_withException_shouldHandleGracefully() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(shippedOrder1, shippedOrder2));

            doThrow(new RuntimeException("确认收货失败"))
                    .doNothing()
                    .when(orderRepository).update(any(OrderAggregate.class));

            orderAutoConfirmTask.autoConfirmReceipt();

            // Both orders were attempted
            verify(orderRepository, times(2)).update(any(OrderAggregate.class));
            // Only the second succeeded past update
            verify(domainEventPublisher, times(1)).publish(any(OrderCompletedEvent.class));
            verify(orderCachePort, times(1)).evictOrderCache(anyLong(), anyLong());
        }

        @Test
        @DisplayName("定时任务禁用时不执行任何操作")
        void autoConfirmReceipt_whenDisabled_shouldDoNothing() {
            when(properties.isEnabled()).thenReturn(false);

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, never()).findShippedOrdersBefore(any());
            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
        }
    }
}
