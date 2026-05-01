package com.cartethyia.easyorange.order.infrastructure.event.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventSubscriber implements DomainEventSubscriber<OrderCreatedEvent> {

    private final ProductInventoryPort productInventoryPort;

    @Override
    public Class<OrderCreatedEvent> getEventType() {
        return OrderCreatedEvent.class;
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        try {
            log.info("开始处理 OrderCreatedEvent: eventId={} productId={}", 
                event.getEventId(), event.getProductId());
            
            productInventoryPort.decreaseStock(event.getProductId());
            
            log.info("OrderCreatedEvent 处理完成：库存已扣减 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderCreatedEvent 处理失败：eventId={} productId={}", 
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
