package com.cartethyia.easyorange.order.infrastructure.event.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundedEventSubscriber implements DomainEventSubscriber<OrderRefundedEvent> {

    private final ProductInventoryPort productInventoryPort;

    @Override
    public Class<OrderRefundedEvent> getEventType() {
        return OrderRefundedEvent.class;
    }

    @Override
    public void handle(OrderRefundedEvent event) {
        try {
            log.info("开始处理 OrderRefundedEvent: eventId={} productId={}", 
                event.getEventId(), event.getProductId());
            
            productInventoryPort.restoreStock(event.getProductId());
            
            log.info("OrderRefundedEvent 处理完成：库存已恢复 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderRefundedEvent 处理失败：eventId={} productId={}", 
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
