package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderTimeoutProperties;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTimeoutTask 单元测试")
class OrderTimeoutTaskTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrderTimeoutProperties properties;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private OrderCachePort orderCachePort;

    @InjectMocks
    private OrderTimeoutTask orderTimeoutTask;

    private static final String ORDER_ID_1 = "100";
    private static final String ORDER_ID_2 = "101";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    private OrderAggregate expiredOrder1;
    private OrderAggregate expiredOrder2;

    @BeforeEach
    void setUp() {
        expiredOrder1 = buildPendingOrder(ORDER_ID_1);
        expiredOrder2 = buildPendingOrder(ORDER_ID_2);
    }

    private OrderAggregate buildPendingOrder(String orderId) {
        return OrderAggregate.fromRaw(
                orderId, "ORD" + orderId,
                BUYER_ID, SELLER_ID,
                java.math.BigDecimal.valueOf(99.99),
                0, 0,
                "地址", "13800138000", "备注",
                null, null
        );
    }

    @Nested
    @DisplayName("cancelExpiredOrders()")
    class CancelExpiredOrdersTests {

        @Test
        @DisplayName("正常取消所有已过期订单")
        void cancelExpiredOrders_shouldCancelAllExpired() throws InterruptedException {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);

            orderTimeoutTask.cancelExpiredOrders();

            verify(orderRepository, times(2)).update(any(OrderAggregate.class));
            verify(domainEventPublisher, times(2)).publish(any(OrderCancelledEvent.class));
            verify(orderCachePort, times(2)).evictOrderCache(anyString(), anyString());
        }

        @Test
        @DisplayName("获取锁失败时跳过该订单，不影响其他订单")
        void cancelExpiredOrders_withLockFailure_shouldSkipOrder() throws InterruptedException {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));
            // First order fails to acquire lock, second succeeds
            RLock lock1 = mock(RLock.class);
            RLock lock2 = mock(RLock.class);
            when(redissonClient.getLock(argThat((String key) -> key != null && key.contains(ORDER_ID_1)))).thenReturn(lock1);
            when(redissonClient.getLock(argThat((String key) -> key != null && key.contains(ORDER_ID_2)))).thenReturn(lock2);
            when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
            when(lock2.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(lock2.isHeldByCurrentThread()).thenReturn(true);

            orderTimeoutTask.cancelExpiredOrders();

            // Only second order should be processed
            verify(orderRepository, times(1)).update(any(OrderAggregate.class));
            verify(domainEventPublisher, times(1)).publish(any(OrderCancelledEvent.class));
            verify(orderCachePort, times(1)).evictOrderCache(anyString(), anyString());
        }

        @Test
        @DisplayName("没有过期订单时不执行任何操作")
        void cancelExpiredOrders_withNoExpiredOrders_shouldDoNothing() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of());

            orderTimeoutTask.cancelExpiredOrders();

            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
            verify(orderCachePort, never()).evictOrderCache(anyString(), anyString());
        }

        @Test
        @DisplayName("部分订单取消失败时继续处理剩余订单")
        void cancelExpiredOrders_withPartialFailure_shouldContinue() throws InterruptedException {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            // First order update throws exception
            doThrow(new RuntimeException("更新失败"))
                    .doNothing()
                    .when(orderRepository).update(any(OrderAggregate.class));

            orderTimeoutTask.cancelExpiredOrders();

            // Both update attempts were made (first fails, second succeeds)
            verify(orderRepository, times(2)).update(any(OrderAggregate.class));
            // Only the second order's event was published (first threw before publish)
            verify(domainEventPublisher, times(1)).publish(any(OrderCancelledEvent.class));
            verify(orderCachePort, times(1)).evictOrderCache(anyString(), anyString());
        }

        @Test
        @DisplayName("定时任务禁用时不执行任何操作")
        void cancelExpiredOrders_whenDisabled_shouldDoNothing() {
            when(properties.isEnabled()).thenReturn(false);

            orderTimeoutTask.cancelExpiredOrders();

            verify(orderRepository, never()).findExpiredOrders(anyInt());
            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
        }
    }
}
