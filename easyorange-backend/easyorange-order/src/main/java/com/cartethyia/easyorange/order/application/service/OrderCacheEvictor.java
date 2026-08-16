package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 订单列表缓存失效器 — 统一「事务提交后再失效」语义，供命令处理器与定时任务共用。
 * <p>
 * 提交前失效会被并发读以旧数据重新填充缓存，因此必须注册 {@code afterCommit} 回调执行。
 */
@Component
@RequiredArgsConstructor
public class OrderCacheEvictor {

    private final OrderCachePort<?> orderCachePort;

    public void evictOrderCacheAfterCommit(Order aggregate) {
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
