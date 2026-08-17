package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderTimeoutProperties;
import com.cartethyia.easyorange.order.application.service.OrderCacheEvictor;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderTimeoutProperties properties;
    private final DistributedLockPort lockPort;
    private final OrderCacheEvictor orderCacheEvictor;
    private final TransactionTemplate transactionTemplate;

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
            boolean cancelledOrder = false;
            try {
                // waitTimeout=0 非阻塞获取：拿不到即跳过，不阻塞扫描；watchdog 覆盖单次取消的全部时长
                // 取消在 TransactionTemplate 事务内执行：状态更新 + OrderCancelledEvent（Outbox）原子提交，
                // 避免「更新已提交、事件未落 Outbox」的崩溃窗口导致库存永不恢复
                cancelledOrder = lockPort.executeWithLocks(
                        List.of(lockKey),
                        0L,
                        () -> transactionTemplate.execute(status -> cancelExpiredOrder(aggregate)));
            } catch (LockAcquisitionException e) {
                log.warn("超时取消获取锁失败/被中断，跳过 orderId={}", aggregate.id().value());
                continue;
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", aggregate.id().value(), e);
                continue;
            }
            if (cancelledOrder) {
                cancelled++;
            }
        }

        log.info("订单超时检查完成: 检查 {} 条, 取消 {} 条", expiredOrders.size(), cancelled);
    }

    private boolean cancelExpiredOrder(Order aggregate) {
        if (!aggregate.canCancel()) {
            return false;
        }

        Transition<Order, OrderCancelledEvent> result = aggregate.cancel("订单超时自动取消", LocalDateTime.now());
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());

        // 缓存提交后再失效，避免提交前失效被并发读以旧数据重新填充
        orderCacheEvictor.evictOrderCacheAfterCommit(aggregate);

        return true;
    }
}
