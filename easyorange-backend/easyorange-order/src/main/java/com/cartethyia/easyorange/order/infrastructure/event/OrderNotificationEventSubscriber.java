package com.cartethyia.easyorange.order.infrastructure.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventSubscriber;
import com.cartethyia.easyorange.common.notification.NotificationService;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.port.outbound.UserInfoPort;
import com.cartethyia.easyorange.order.domain.port.outbound.UserInfoPort.UserInfo;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventSubscriber implements DomainEventSubscriber<BaseDomainEvent> {

    private final EventIdempotencyChecker idempotencyChecker;
    private final NotificationService notificationService;
    private final OrderReadRepository orderReadRepository;
    private final UserInfoPort userInfoPort;

    @Override
    public Class<BaseDomainEvent> getEventType() {
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
            } else if (event instanceof OrderRefundedEvent e) {
                handleOrderRefunded(e);
            }
        } catch (Exception e) {
            log.error("action=handle_notification_failed eventType={} eventId={}", eventType, eventId, e);
            throw e;
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        String email = getUserEmail(event.getBuyerId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已创建", "您的订单已创建，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_created orderId={}", event.getOrderId());
    }

    private void handleOrderPaid(OrderPaidEvent event) {
        String email = getEmailFromOrder(event.getOrderId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已支付", "您的订单已支付成功，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_paid orderId={}", event.getOrderId());
    }

    private void handleOrderShipped(OrderShippedEvent event) {
        String email = getEmailFromOrder(event.getOrderId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已发货", "您的订单已发货，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_shipped orderId={}", event.getOrderId());
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        String email = getEmailFromOrder(event.getOrderId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已完成", "您的订单已完成，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_completed orderId={}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        String email = getEmailFromOrder(event.getOrderId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已取消", "您的订单已取消，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_cancelled orderId={}", event.getOrderId());
    }

    private void handleOrderRefunded(OrderRefundedEvent event) {
        String email = getEmailFromOrder(event.getOrderId());
        if (email != null) {
            notificationService.sendEmail(email, "订单已退款", "您的订单已退款，订单号: " + event.getOrderId());
        }
        log.info("action=notify_order_refunded orderId={}", event.getOrderId());
    }

    private String getUserEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        return userInfoPort.getUserInfo(userId)
                .map(UserInfo::email)
                .orElseGet(() -> {
                    log.warn("action=user_not_found userId={}", userId);
                    return null;
                });
    }

    private String getEmailFromOrder(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return orderReadRepository.findById(OrderId.of(orderId))
                .map(readModel -> getUserEmail(readModel.buyerId()))
                .orElse(null);
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
        } else if (event instanceof OrderRefundedEvent e) {
            return "refunded:" + e.getOrderId() + ":" + e.getProductId();
        }
        return event.getClass().getSimpleName() + ":" + System.currentTimeMillis();
    }
}
