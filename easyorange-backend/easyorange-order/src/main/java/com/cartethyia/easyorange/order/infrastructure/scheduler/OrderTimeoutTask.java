package com.cartethyia.easyorange.order.infrastructure.scheduler;

import com.cartethyia.easyorange.order.constant.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;
    private final RedisCache redisCache;

    private static final String CANCEL_LOCK_PREFIX = "eo:order:lock:cancel:";

    @Scheduled(cron = "${order.timeout.cron:0 */5 * * * ?}")
    public void cancelExpiredOrders() {
        if (!properties.isEnabled()) {
            return;
        }

        List<OrderAggregate> expiredOrders = orderRepository.findExpiredOrders(properties.getTimeoutMinutes());
        if (expiredOrders.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (OrderAggregate aggregate : expiredOrders) {
            String lockKey = CANCEL_LOCK_PREFIX + aggregate.id().value();
            String lockValue = UUID.randomUUID().toString();

            Boolean locked = redisCache.tryLock(lockKey, lockValue, 30, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
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
                redisCache.unlockIfValueMatches(lockKey, lockValue);
            }
        }

        log.info("订单超时检查完成: 检查 {} 条, 取消 {} 条", expiredOrders.size(), cancelled);
    }

    private boolean cancelExpiredOrder(OrderAggregate aggregate) {
        if (!aggregate.canCancel()) {
            return false;
        }

        OrderAggregate.OrderCancelledResult result = aggregate.cancel("订单超时自动取消");
        orderRepository.update(result.aggregate());

        domainEventPublisher.publish(result.event());

        return true;
    }
}
