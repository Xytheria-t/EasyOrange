package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderAutoConfirmProperties;
import com.cartethyia.easyorange.order.application.service.OrderCacheEvictor;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderAutoConfirmProperties properties;
    private final OrderCacheEvictor orderCacheEvictor;
    private final OrderStateMigrationExecutor migrationExecutor;

    private static final String CONFIRM_LOCK_PREFIX = "eo:order:lock:confirm:";

    @Scheduled(cron = "${order.auto-confirm.cron:0 0 2 * * ?}")
    public void autoConfirmReceipt() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(properties.getAutoConfirmDays());
        List<Order> shippedOrders = orderRepository.findShippedOrdersBefore(threshold);
        migrationExecutor.execute("自动确认收货", CONFIRM_LOCK_PREFIX, shippedOrders, this::autoConfirmOrder);
    }

    private boolean autoConfirmOrder(Order aggregate) {
        if (!aggregate.canConfirmReceipt()) {
            return false;
        }

        Transition<Order, OrderCompletedEvent> result = aggregate.confirmReceipt(LocalDateTime.now());
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());

        // 缓存提交后再失效，避免提交前失效被并发读以旧数据重新填充
        orderCacheEvictor.evictOrderCacheAfterCommit(aggregate);

        return true;
    }
}
