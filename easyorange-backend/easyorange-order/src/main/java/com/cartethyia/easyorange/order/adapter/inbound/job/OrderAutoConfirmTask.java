package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.constant.OrderConstant;
import com.cartethyia.easyorange.order.infrastructure.config.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.port.output.OrderRepository;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;
    private final OrderCachePort<?> orderCachePort;

    @Scheduled(cron = "${order.auto-confirm.cron:0 0 2 * * ?}")
    public void autoConfirmReceipt() {
        if (!properties.isEnabled()) {
            return;
        }

        long autoConfirmDays = OrderConstant.AUTO_CONFIRM_DAYS;
        LocalDateTime threshold = LocalDateTime.now().minusDays(autoConfirmDays);

        List<OrderAggregate> shippedOrders = orderRepository.findShippedOrdersBefore(threshold);
        if (shippedOrders.isEmpty()) {
            return;
        }

        int confirmed = 0;
        for (OrderAggregate aggregate : shippedOrders) {
            try {
                if (autoConfirmOrder(aggregate)) {
                    confirmed++;
                }
            } catch (Exception e) {
                log.error("自动确认收货失败: orderId={}", aggregate.id().value(), e);
            }
        }

        log.info("自动确认收货检查完成: 检查 {} 条, 确认 {} 条", shippedOrders.size(), confirmed);
    }

    private boolean autoConfirmOrder(OrderAggregate aggregate) {
        if (!aggregate.canConfirmReceipt()) {
            return false;
        }

        OrderAggregate.OrderCompletedResult result = aggregate.confirmReceipt();
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());

        return true;
    }
}
