package com.cartethyia.easyorange.order.service;

import com.cartethyia.easyorange.order.constant.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;

    @Scheduled(cron = "${order.timeout.cron:0 */5 * * * ?}")
    public void cancelExpiredOrders() {
        if (!properties.isEnabled()) {
            return;
        }

        List<Order> expiredOrders = orderRepository.findExpiredOrders(properties.getTimeoutMinutes());
        if (expiredOrders.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (Order order : expiredOrders) {
            try {
                if (cancelExpiredOrder(order)) {
                    cancelled++;
                    log.info("订单超时自动取消: orderNo={}, productId={}", order.getOrderNo(), order.getProductId());
                }
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", order.getId(), e);
            }
        }

        log.info("订单超时检查完成: 检查 {} 条, 取消 {} 条", expiredOrders.size(), cancelled);
    }

    private boolean cancelExpiredOrder(Order order) {
        OrderAggregate aggregate = OrderAggregate.fromEntity(order);
        if (!OrderStatus.canCancel(aggregate.getStatus())) {
            return false;
        }

        OrderCancelledEvent event = aggregate.cancel("订单超时自动取消");
        orderRepository.update(aggregate.toEntity());

        domainEventPublisher.publish(event);

        return true;
    }
}