package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.port.output.ProductInventoryPort;
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
            productInventoryPort.restoreStock(event.getProductId());
        } catch (Exception e) {
            log.error("OrderRefundedEvent 处理失败：eventId={} productId={}",
                event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
