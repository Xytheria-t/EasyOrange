package com.cartethyia.easyorange.order.adapter.inbound.job;

import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.orderWithStatus;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.application.service.OrderCacheEvictor;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
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
@DisplayName("OrderTimeoutTask 单元测试")
class OrderTimeoutTaskTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrderTimeoutProperties properties;

    @Mock
    private DistributedLockPort lockPort;

    @Mock
    private OrderCacheEvictor orderCacheEvictor;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderStateMigrationExecutor migrationExecutor;

    private OrderTimeoutTask orderTimeoutTask;

    private static final String ORDER_ID_1 = "100";
    private static final String ORDER_ID_2 = "101";

    private Order expiredOrder1;
    private Order expiredOrder2;

    @BeforeEach
    void setUp() {
        migrationExecutor = new OrderStateMigrationExecutor(lockPort, transactionTemplate);
        orderTimeoutTask = new OrderTimeoutTask(
                orderRepository, domainEventPublisher, properties, orderCacheEvictor, migrationExecutor);
        expiredOrder1 = orderWithStatus(ORDER_ID_1, OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID);
        expiredOrder2 = orderWithStatus(ORDER_ID_2, OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID);
        // 默认锁端口正常：直接执行锁内操作（获取锁成功），返回其 boolean 结果
        when(lockPort.executeWithLocks(anyList(), anyLong(), any()))
                .thenAnswer(inv -> ((DistributedLockPort.LockOperation<?>) inv.getArgument(2)).execute());
        // 事务模板直接执行回调（事务行为由真实事务路径的集成测试覆盖）
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Nested
    @DisplayName("cancelExpiredOrders()")
    class CancelExpiredOrdersTests {

        @Test
        @DisplayName("正常取消所有已过期订单")
        void cancelExpiredOrders_shouldCancelAllExpired() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));

            orderTimeoutTask.cancelExpiredOrders();

            verify(orderRepository, times(2)).update(any(Order.class));
            verify(domainEventPublisher, times(2)).publish(any(OrderCancelledEvent.class));
            verify(orderCacheEvictor, times(2)).evictOrderCacheAfterCommit(any());
        }

        @Test
        @DisplayName("获取锁失败时跳过该订单，不影响其他订单")
        void cancelExpiredOrders_withLockFailure_shouldSkipOrder() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));
            // First order fails to acquire lock, second succeeds（doThrow 风格：避免 when() 内先调用命中 setUp 的 thenAnswer 桩）
            doThrow(new LockAcquisitionException("busy"))
                    .doAnswer(inv -> ((DistributedLockPort.LockOperation<?>) inv.getArgument(2)).execute())
                    .when(lockPort)
                    .executeWithLocks(anyList(), anyLong(), any());

            orderTimeoutTask.cancelExpiredOrders();

            // Only second order should be processed
            verify(orderRepository, times(1)).update(any(Order.class));
            verify(domainEventPublisher, times(1)).publish(any(OrderCancelledEvent.class));
            verify(orderCacheEvictor, times(1)).evictOrderCacheAfterCommit(any());
        }

        @Test
        @DisplayName("没有过期订单时不执行任何操作")
        void cancelExpiredOrders_withNoExpiredOrders_shouldDoNothing() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of());

            orderTimeoutTask.cancelExpiredOrders();

            verify(orderRepository, never()).update(any());
            verify(domainEventPublisher, never()).publish(any());
            verify(orderCacheEvictor, never()).evictOrderCacheAfterCommit(any());
        }

        @Test
        @DisplayName("部分订单取消失败时继续处理剩余订单")
        void cancelExpiredOrders_withPartialFailure_shouldContinue() {
            when(properties.isEnabled()).thenReturn(true);
            when(orderRepository.findExpiredOrders(anyInt())).thenReturn(List.of(expiredOrder1, expiredOrder2));
            // First order update throws exception
            doThrow(new RuntimeException("更新失败"))
                    .doNothing()
                    .when(orderRepository)
                    .update(any(Order.class));

            orderTimeoutTask.cancelExpiredOrders();

            // Both update attempts were made (first fails, second succeeds)
            verify(orderRepository, times(2)).update(any(Order.class));
            // Only the second order's event was published (first threw before publish)
            verify(domainEventPublisher, times(1)).publish(any(OrderCancelledEvent.class));
            verify(orderCacheEvictor, times(1)).evictOrderCacheAfterCommit(any());
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
