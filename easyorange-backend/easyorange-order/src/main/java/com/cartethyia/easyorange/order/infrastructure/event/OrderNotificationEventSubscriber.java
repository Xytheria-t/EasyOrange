package com.cartethyia.easyorange.order.infrastructure.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventSubscriber implements DomainEventSubscriber {

    private final EventIdempotencyChecker idempotencyChecker;

    @Override
    public Class<? extends BaseDomainEvent> getEventType() {
        return BaseDomainEvent.class;
    }

    @Override
    public void handle(BaseDomainEvent event) {
        String eventId = getEventId(event);
        String eventType = event.getClass().getSimpleName();

        if (idempotencyChecker.isDuplicate(eventType, eventId)) {
            log.warn("action=skip_duplicate_notification eventType={} eventId={}", eventType, eventId);
            return;
        }

        if (!idempotencyChecker.tryMark(eventType, eventId)) {
            log.warn("action=notification_already_processing eventType={} eventId={}", eventType, eventId);
            return;
        }

        try {
            if (event instanceof OrderCreatedEvent e) {
                handleOrderCreated(e);
            } else if (event instanceof OrderPaidEvent e) {
                handleOrderPaid(e);
            } else if (event instanceof OrderShippedEvent e) {
                handleOrderShipped(e);
            } else if (event instanceof OrderCompletedEvent e) {
                handleOrderCompleted(e);
            } else if (event instanceof OrderCancelledEvent e) {
                handleOrderCancelled(e);
            }
        } catch (Exception e) {
            log.error("action=handle_notification_failed eventType={} eventId={}", eventType, eventId, e);
            throw e;
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        // TODO: Implement notification service
        log.info("action=notify_order_created orderId={}", event.getOrderId());
    }

    private void handleOrderPaid(OrderPaidEvent event) {
        // TODO: Implement notification service
        log.info("action=notify_order_paid orderId={}", event.getOrderId());
    }

    private void handleOrderShipped(OrderShippedEvent event) {
        // TODO: Implement notification service
        log.info("action=notify_order_shipped orderId={}", event.getOrderId());
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        // TODO: Implement notification service
        log.info("action=notify_order_completed orderId={}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        // TODO: Implement notification service
        log.info("action=notify_order_cancelled orderId={}", event.getOrderId());
    }

    private String getEventId(BaseDomainEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            return "created:" + e.getOrderId();
        } else if (event instanceof OrderPaidEvent e) {
            return "paid:" + e.getOrderId();
        } else if (event instanceof OrderShippedEvent e) {
            return "shipped:" + e.getOrderId();
        } else if (event instanceof OrderCompletedEvent e) {
            return "completed:" + e.getOrderId();
        } else if (event instanceof OrderCancelledEvent e) {
            return "cancelled:" + e.getOrderId() + ":" + e.getProductId();
        }
        return event.getClass().getSimpleName() + ":" + System.currentTimeMillis();
    }
}
