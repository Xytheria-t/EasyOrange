package com.cartethyia.easyorange.order.adapter.inbound.job;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.application.service.OrderCacheEvictor;
import com.cartethyia.easyorange.order.adapter.outbound.config.OrderAutoConfirmProperties;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
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
public class OrderAutoConfirmTask {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderAutoConfirmProperties properties;
    private final DistributedLockPort lockPort;
    private final OrderCacheEvictor orderCacheEvictor;
    private final TransactionTemplate transactionTemplate;

    private static final String CONFIRM_LOCK_PREFIX = "eo:order:lock:confirm:";

    @Scheduled(cron = "${order.auto-confirm.cron:0 0 2 * * ?}")
    public void autoConfirmReceipt() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(properties.getAutoConfirmDays());
        List<Order> shippedOrders = orderRepository.findShippedOrdersBefore(threshold);
        if (shippedOrders.isEmpty()) {
            return;
        }

        int confirmed = 0;
        for (Order aggregate : shippedOrders) {
            String lockKey = CONFIRM_LOCK_PREFIX + aggregate.id().value();
            try {
                // waitTimeout=0 非阻塞获取：拿不到即跳过，不阻塞扫描；watchdog 覆盖单次确认的全部时长
                // 确认收货在 TransactionTemplate 事务内执行：状态更新 + OrderCompletedEvent（Outbox）原子提交，
                // 避免「更新已提交、事件未落 Outbox」的崩溃窗口导致商品漏标记售出
                boolean confirmedOrder = lockPort.executeWithLocks(List.of(lockKey), 0L,
                        () -> transactionTemplate.execute(status -> autoConfirmOrder(aggregate)));
                if (confirmedOrder) {
                    confirmed++;
                }
            } catch (LockAcquisitionException e) {
                log.warn("自动确认收货获取锁失败/被中断，跳过 orderId={}", aggregate.id().value());
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
        orderCacheEvictor.evictOrderCacheAfterCommit(aggregate);

        return true;
    }
}
