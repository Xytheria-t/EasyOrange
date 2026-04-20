package com.cartethyia.easyorange.order.infrastructure.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventRegistry;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.message.service.NotificationService;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventSubscriber implements DomainEventSubscriber<BaseDomainEvent> {

    private final NotificationService notificationService;
    private final EventIdempotencyChecker idempotencyChecker;
    private final DomainEventRegistry domainEventRegistry;

    @PostConstruct
    public void register() {
        domainEventRegistry.subscribe(OrderCreatedEvent.class.getSimpleName(), this);
        domainEventRegistry.subscribe(OrderPaidEvent.class.getSimpleName(), this);
        domainEventRegistry.subscribe(OrderShippedEvent.class.getSimpleName(), this);
        domainEventRegistry.subscribe(OrderCompletedEvent.class.getSimpleName(), this);
        domainEventRegistry.subscribe(OrderCancelledEvent.class.getSimpleName(), this);
        log.info("OrderNotificationEventSubscriber registered");
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
        notificationService.broadcastNotification(
                "订单创建成功",
                String.format("您有一笔新订单，订单号：%s，金额：%s", event.getOrderId(), event.getAmount()),
                List.of(event.getSellerId())
        );
        log.info("action=notify_order_created orderId={} sellerId={}", event.getOrderId(), event.getSellerId());
    }

    private void handleOrderPaid(OrderPaidEvent event) {
        notificationService.broadcastNotification(
                "订单已支付",
                String.format("订单 %s 已支付成功", event.getOrderId()),
                List.of(event.getOrderId())
        );
        log.info("action=notify_order_paid orderId={}", event.getOrderId());
    }

    private void handleOrderShipped(OrderShippedEvent event) {
        notificationService.broadcastNotification(
                "订单已发货",
                String.format("订单 %s 已发货，请注意查收", event.getOrderId()),
                List.of(event.getOrderId())
        );
        log.info("action=notify_order_shipped orderId={}", event.getOrderId());
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        notificationService.broadcastNotification(
                "订单已完成",
                String.format("订单 %s 已确认收货，交易完成", event.getOrderId()),
                List.of(event.getOrderId())
        );
        log.info("action=notify_order_completed orderId={}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        notificationService.broadcastNotification(
                "订单已取消",
                String.format("订单 %s 已取消", event.getOrderId()),
                List.of(event.getOrderId())
        );
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
