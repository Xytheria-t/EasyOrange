package com.cartethyia.easyorange.order.adapter.inbound.job;

import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.orderWithStatus;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderAutoConfirmProperties;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.adapter.outbound.cache.OrderCacheEvictor;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderAutoConfirmTask 单元测试")
class OrderAutoConfirmTaskTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrderAutoConfirmProperties properties;

    @Mock
    private DistributedLockPort lockPort;

    @Mock
    private OrderCacheEvictor orderCacheEvictor;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private OrderAutoConfirmTask orderAutoConfirmTask;

    private static final String ORDER_ID_1 = "100";
    private static final String ORDER_ID_2 = "101";

    private Order shippedOrder1;
    private Order shippedOrder2;

    @BeforeEach
    void setUp() {
        shippedOrder1 = orderWithStatus(ORDER_ID_1, OrderStatus.SHIPPED, PaymentStatus.PAID);
        shippedOrder2 = orderWithStatus(ORDER_ID_2, OrderStatus.SHIPPED, PaymentStatus.PAID);
        // 事务模板直接执行回调（事务行为由真实事务路径的集成测试覆盖）
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        // 默认锁端口正常：直接执行锁内操作
        when(lockPort.executeWithLocks(anyList(), anyLong(), any()))
                .thenAnswer(inv -> ((DistributedLockPort.LockOperation<?>) inv.getArgument(2)).execute());
        when(properties.isEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("autoConfirmReceipt()")
    class AutoConfirmReceiptTests {

        @Test
        @DisplayName("自动确认所有已发货订单")
        void autoConfirmReceipt_shouldConfirmAll() {
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(shippedOrder1, shippedOrder2));

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, times(2)).update(any(Order.class));
            verify(domainEventPublisher, times(2)).publish(any(OrderCompletedEvent.class));
            verify(orderCacheEvictor, times(2)).evictOrderCacheAfterCommit(any());
            verify(lockPort, times(2)).executeWithLocks(anyList(), anyLong(), any());
        }

        @Test
        @DisplayName("没有已发货订单时不执行任何操作")
        void autoConfirmReceipt_withNoShippedOrders_shouldDoNothing() {
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
            verify(orderCacheEvictor, never()).evictOrderCacheAfterCommit(any());
        }

        @Test
        @DisplayName("处理异常时优雅跳过，不影响其他订单")
        void autoConfirmReceipt_withException_shouldHandleGracefully() {
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(shippedOrder1, shippedOrder2));

            doThrow(new RuntimeException("确认收货失败"))
                    .doNothing()
                    .when(orderRepository)
                    .update(any(Order.class));

            orderAutoConfirmTask.autoConfirmReceipt();

            // Both orders were attempted
            verify(orderRepository, times(2)).update(any(Order.class));
            // Only the second succeeded past update
            verify(domainEventPublisher, times(1)).publish(any(OrderCompletedEvent.class));
            verify(orderCacheEvictor, times(1)).evictOrderCacheAfterCommit(any());
        }

        @Test
        @DisplayName("获取分布式锁失败时跳过该订单，不阻塞其他订单")
        void autoConfirmReceipt_lockFailed_shouldSkipOrder() {
            when(orderRepository.findShippedOrdersBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(shippedOrder1, shippedOrder2));
            doThrow(new LockAcquisitionException("busy"))
                    .doAnswer(inv -> ((DistributedLockPort.LockOperation<?>) inv.getArgument(2)).execute())
                    .when(lockPort)
                    .executeWithLocks(anyList(), anyLong(), any());

            orderAutoConfirmTask.autoConfirmReceipt();

            verify(orderRepository, times(1)).update(any(Order.class));
            verify(domainEventPublisher, times(1)).publish(any(OrderCompletedEvent.class));
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
