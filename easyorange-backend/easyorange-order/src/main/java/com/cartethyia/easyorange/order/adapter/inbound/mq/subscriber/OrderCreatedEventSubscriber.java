package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.StockReservationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventSubscriber {

    private final DomainEventPublisher domainEventPublisher;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: orderId={}, productId={}", event.getOrderId(), event.getProductId());
        
        try {
            StockReservationRequestedEvent stockEvent = new StockReservationRequestedEvent(
                    event.getOrderId(),
                    event.getProductId(),
                    1
            );
            
            domainEventPublisher.publish(stockEvent);
            
            log.info("库存预留请求已发布: orderId={}, productId={}", event.getOrderId(), event.getProductId());
        } catch (Exception e) {
            log.error("发布库存预留请求失败: orderId={}, productId={}", event.getOrderId(), event.getProductId(), e);
        }
    }
}
