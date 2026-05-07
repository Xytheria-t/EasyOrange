package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.port.output.ProductInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventSubscriber implements DomainEventSubscriber<OrderCancelledEvent> {

    private final ProductInventoryPort productInventoryPort;

    @Override
    public Class<OrderCancelledEvent> getEventType() {
        return OrderCancelledEvent.class;
    }

    @Override
    public void handle(OrderCancelledEvent event) {
        try {
            productInventoryPort.restoreStock(event.getProductId());
        } catch (Exception e) {
            log.error("OrderCancelledEvent 处理失败：eventId={} productId={}",
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
