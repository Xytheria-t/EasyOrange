package com.cartethyia.easyorange.order.infrastructure.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.product.application.command.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.RestoreStockCommand;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventSubscriber implements DomainEventSubscriber<BaseDomainEvent> {

    private final ProductCommandHandler productCommandHandler;

    @Override
    public Class<BaseDomainEvent> getEventType() {
        return BaseDomainEvent.class;
    }

    @Override
    public void handle(BaseDomainEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            handleOrderCreated(e);
        } else if (event instanceof OrderCancelledEvent e) {
            handleOrderCancelled(e);
        } else if (event instanceof OrderCompletedEvent e) {
            handleOrderCompleted(e);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("开始处理 OrderCreatedEvent: eventId={} productId={}", event.getEventId(), event.getProductId());
            productCommandHandler.handle(new DecrementStockCommand(event.getProductId()));
            log.info("OrderCreatedEvent 处理完成：库存已扣减 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderCreatedEvent 处理失败：eventId={} productId={}", event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        try {
            log.info("开始处理 OrderCancelledEvent: eventId={} productId={}", event.getEventId(), event.getProductId());
            productCommandHandler.handle(new RestoreStockCommand(event.getProductId()));
            log.info("OrderCancelledEvent 处理完成：库存已恢复 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderCancelledEvent 处理失败：eventId={} productId={}", event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            log.info("开始处理 OrderCompletedEvent: eventId={} productId={}", event.getEventId(), event.getProductId());
            productCommandHandler.handle(new MarkAsSoldCommand(event.getProductId()));
            log.info("OrderCompletedEvent 处理完成：商品已标记为已售 productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("OrderCompletedEvent 处理失败：eventId={} productId={}", event.getEventId(), event.getProductId(), e);
            throw e;
        }
    }
}
