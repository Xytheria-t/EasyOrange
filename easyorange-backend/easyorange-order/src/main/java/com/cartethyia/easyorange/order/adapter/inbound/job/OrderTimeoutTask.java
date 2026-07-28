package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;
    private final RedissonClient redissonClient;
    private final OrderCachePort<?> orderCachePort;

    private static final String CANCEL_LOCK_PREFIX = "eo:order:lock:cancel:";

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
        for (Order aggregate : expiredOrders) {
            String lockKey = CANCEL_LOCK_PREFIX + aggregate.id().value();
            RLock lock = redissonClient.getLock(lockKey);

            boolean locked;
            try {
                locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("超时取消获取锁被中断，跳过 orderId={}", aggregate.id().value());
                continue;
            }
            if (!locked) {
                log.warn("超时取消获取锁失败，跳过 orderId={}", aggregate.id().value());
                continue;
            }

            try {
                if (cancelExpiredOrder(aggregate)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", aggregate.id().value(), e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        log.info("订单超时检查完成: 检查 {} 条, 取消 {} 条", expiredOrders.size(), cancelled);
    }

    private boolean cancelExpiredOrder(Order aggregate) {
        if (!aggregate.canCancel()) {
            return false;
        }

        Order.OrderTransition<OrderCancelledEvent> result = aggregate.cancel("订单超时自动取消");
        orderRepository.update(result.aggregate());

        orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
        domainEventPublisher.publish(result.event());

        return true;
    }
}
