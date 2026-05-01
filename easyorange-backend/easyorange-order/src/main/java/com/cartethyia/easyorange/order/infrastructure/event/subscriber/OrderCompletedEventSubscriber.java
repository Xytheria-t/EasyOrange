package com.cartethyia.easyorange.order.infrastructure.event.subscriber;

import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventSubscriber implements DomainEventSubscriber<OrderCompletedEvent> {

    private final ProductInventoryPort productInventoryPort;

    @Override
    public Class<OrderCompletedEvent> getEventType() {
        return OrderCompletedEvent.class;
    }

    @Override
    public void handle(OrderCompletedEvent event) {
        try {
            log.info("开始处理 OrderCompletedEvent: eventId={} productId={}", 
                event.getEventId(), event.getProductId());
            
            productInventoryPort.markAsSold(event.getProductId());
            
            log.info("OrderCompletedEvent 处理完成：商品已标记为已售 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderCompletedEvent 处理失败：eventId={} productId={}", 
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
