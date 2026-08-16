package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderConstant;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;
    private final OrderCachePort<?> orderCachePort;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "${order.auto-confirm.cron:0 0 2 * * ?}")
    public void autoConfirmReceipt() {
        if (!properties.isEnabled()) {
            return;
        }

        long autoConfirmDays = OrderConstant.AUTO_CONFIRM_DAYS;
        LocalDateTime threshold = LocalDateTime.now().minusDays(autoConfirmDays);

        List<Order> shippedOrders = orderRepository.findShippedOrdersBefore(threshold);
        if (shippedOrders.isEmpty()) {
            return;
        }

        int confirmed = 0;
        for (Order aggregate : shippedOrders) {
            try {
                // 确认收货在本地事务内执行：状态更新 + OrderCompletedEvent（Outbox）原子提交，
                // 避免「更新已提交、事件未落 Outbox」的崩溃窗口导致商品漏标记售出
                if (transactionTemplate.execute(status -> autoConfirmOrder(aggregate))) {
                    confirmed++;
                }
            } catch (Exception e) {
                log.error("自动确认收货失败: orderId={}", aggregate.id().value(), e);
            }
        }

        log.info("自动确认收货检查完成: 检查 {} 条, 确认 {} 条", shippedOrders.size(), confirmed);
    }

    private boolean autoConfirmOrder(Order aggregate) {
        if (!aggregate.canConfirmReceipt()) {
            return false;
        }

        Transition<Order, OrderCompletedEvent> result = aggregate.confirmReceipt(LocalDateTime.now());
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());

        // 缓存提交后再失效，避免提交前失效被并发读以旧数据重新填充
        evictOrderCacheAfterCommit(aggregate);

        return true;
    }

    private void evictOrderCacheAfterCommit(Order aggregate) {
        var buyerId = aggregate.buyerId().value();
        var sellerId = aggregate.sellerId().value();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderCachePort.evictOrderCache(buyerId, sellerId);
                }
            });
        } else {
            orderCachePort.evictOrderCache(buyerId, sellerId);
        }
    }
}
