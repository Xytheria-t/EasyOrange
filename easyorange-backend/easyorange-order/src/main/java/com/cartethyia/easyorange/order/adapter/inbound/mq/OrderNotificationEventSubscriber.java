package com.cartethyia.easyorange.order.adapter.inbound.mq;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.notification.NotificationService;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.port.UserInfoPort;
import com.cartethyia.easyorange.order.domain.port.UserInfoPort.UserInfo;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventSubscriber {

    private final EventIdempotencyChecker idempotencyChecker;
    private final NotificationService notificationService;
    private final OrderReadRepository orderReadRepository;
    private final UserInfoPort userInfoPort;

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        String eventId = "created:" + event.getOrderId();
        if (!tryAcquireLock("OrderCreated", eventId)) {
            return;
        }
        
        try {
            String email = getUserEmail(event.getBuyerId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已创建", "您的订单已创建，订单号: " + event.getOrderId());
            }
            log.info("订单创建通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单创建通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        String eventId = "paid:" + event.getOrderId();
        if (!tryAcquireLock("OrderPaid", eventId)) {
            return;
        }
        
        try {
            String email = getEmailFromOrder(event.getOrderId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已支付", "您的订单已支付成功，订单号: " + event.getOrderId());
            }
            log.info("订单支付通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单支付通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderShipped(OrderShippedEvent event) {
        String eventId = "shipped:" + event.getOrderId();
        if (!tryAcquireLock("OrderShipped", eventId)) {
            return;
        }
        
        try {
            String email = getEmailFromOrder(event.getOrderId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已发货", "您的订单已发货，订单号: " + event.getOrderId());
            }
            log.info("订单发货通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单发货通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        String eventId = "completed:" + event.getOrderId();
        if (!tryAcquireLock("OrderCompleted", eventId)) {
            return;
        }
        
        try {
            String email = getEmailFromOrder(event.getOrderId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已完成", "您的订单已完成，订单号: " + event.getOrderId());
            }
            log.info("订单完成通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单完成通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        String eventId = "cancelled:" + event.getOrderId();
        if (!tryAcquireLock("OrderCancelled", eventId)) {
            return;
        }
        
        try {
            String email = getEmailFromOrder(event.getOrderId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已取消", "您的订单已取消，订单号: " + event.getOrderId());
            }
            log.info("订单取消通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单取消通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderRefunded(OrderRefundedEvent event) {
        String eventId = "refunded:" + event.getOrderId();
        if (!tryAcquireLock("OrderRefunded", eventId)) {
            return;
        }
        
        try {
            String email = getEmailFromOrder(event.getOrderId());
            if (email != null) {
                notificationService.sendEmail(email, "订单已退款", "您的订单已退款，订单号: " + event.getOrderId());
            }
            log.info("订单退款通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单退款通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    private boolean tryAcquireLock(String eventType, String eventId) {
        if (idempotencyChecker.isDuplicate(eventType, eventId)) {
            return false;
        }
        return idempotencyChecker.tryMark(eventType, eventId);
    }

    private String getUserEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        return userInfoPort.getUserInfo(userId)
                .map(UserInfo::email)
                .orElse(null);
    }

    private String getEmailFromOrder(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return orderReadRepository.findById(OrderId.of(orderId))
                .map(readModel -> getUserEmail(readModel.buyerId()))
                .orElse(null);
    }
}
