package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
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
public class OrderRefundedEventSubscriber {

    private final ProductInventoryPort productInventoryPort;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderRefunded(OrderRefundedEvent event) {
        log.info("收到订单退款事件: orderId={}, productId={}", event.getOrderId(), event.getProductId());
        
        try {
            productInventoryPort.restoreStock(event.getProductId());
            log.info("库存恢复成功: productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("库存恢复失败: orderId={}, productId={}", event.getOrderId(), event.getProductId(), e);
        }
    }
}
