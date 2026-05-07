package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.port.output.ProductInventoryPort;
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
            productInventoryPort.decreaseStock(event.getProductId());
        } catch (Exception e) {
            log.error("OrderCreatedEvent 处理失败：eventId={} productId={}",
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
