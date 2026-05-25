package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.port.output.ProductInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventSubscriber {

    private final ProductInventoryPort productInventoryPort;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("收到订单完成事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());

        for (Long productId : event.getProductIds()) {
            try {
                productInventoryPort.markAsSold(productId);
                log.info("商品标记已售成功: productId={}", productId);
            } catch (Exception e) {
                log.error("商品标记已售失败: orderId={}, productId={}", event.getOrderId(), productId, e);
            }
        }
    }
}
