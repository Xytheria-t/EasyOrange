package com.cartethyia.easyorange.product.application.event;

import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("处理商品创建事件：productId={}, userId={}, name={}",
                event.getProductId(), event.getUserId(), event.getName());

        // TODO: 发送通知、更新搜索索引、同步到推荐系统等
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("处理商品更新事件：productId={}", event.getProductId());

        // TODO: 更新搜索索引、同步到推荐系统等
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("处理商品删除事件：productId={}", event.getProductId());

        // TODO: 清理搜索索引、更新推荐系统等
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductMarkedSold(ProductMarkedSoldEvent event) {
        log.info("处理商品售出事件：productId={}", event.getProductId());

        // TODO: 发送通知、更新统计数据、更新推荐系统等
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockDecreased(StockDecreasedEvent event) {
        log.info("处理库存扣减事件：productId={}, quantity={}",
                event.getProductId(), event.getQuantity());

        // TODO: 库存预警检查、同步库存数据等
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockRestored(StockRestoredEvent event) {
        log.info("处理库存恢复事件：productId={}, quantity={}",
                event.getProductId(), event.getQuantity());

        // TODO: 同步库存数据等
    }
}
