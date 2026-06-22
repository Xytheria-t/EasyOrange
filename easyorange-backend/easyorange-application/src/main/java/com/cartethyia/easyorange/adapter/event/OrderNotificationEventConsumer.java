package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(
    queues = RabbitMQConfig.QUEUE_ORDER_NOTIFICATION,
    containerFactory = "domainEventContainerFactory"
)
public class OrderNotificationEventConsumer {

    private final EventIdempotencyChecker idempotencyChecker;
    private final MessageCommandHandler messageCommandHandler;
    private final OrderReadRepository orderReadRepository;

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        String eventId = "created:" + event.getOrderId();
        if (!tryAcquireLock("OrderCreated", eventId)) {
            return;
        }

        try {
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(event.getBuyerId())
                    .title("订单已创建")
                    .content("您的订单已创建，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单创建通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单创建通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderPaid(OrderPaidEvent event) {
        String eventId = "paid:" + event.getOrderId();
        if (!tryAcquireLock("OrderPaid", eventId)) {
            return;
        }

        try {
            Long buyerId = getBuyerId(event.getOrderId());
            if (buyerId == null) {
                log.warn("订单不存在，跳过支付通知: orderId={}", event.getOrderId());
                return;
            }
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(buyerId)
                    .title("订单已支付")
                    .content("您的订单已支付成功，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单支付通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单支付通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderShipped(OrderShippedEvent event) {
        String eventId = "shipped:" + event.getOrderId();
        if (!tryAcquireLock("OrderShipped", eventId)) {
            return;
        }

        try {
            Long buyerId = getBuyerId(event.getOrderId());
            if (buyerId == null) {
                log.warn("订单不存在，跳过发货通知: orderId={}", event.getOrderId());
                return;
            }
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(buyerId)
                    .title("订单已发货")
                    .content("您的订单已发货，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单发货通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单发货通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) {
        String eventId = "completed:" + event.getOrderId();
        if (!tryAcquireLock("OrderCompleted", eventId)) {
            return;
        }

        try {
            Long buyerId = getBuyerId(event.getOrderId());
            if (buyerId == null) {
                log.warn("订单不存在，跳过完成通知: orderId={}", event.getOrderId());
                return;
            }
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(buyerId)
                    .title("订单已完成")
                    .content("您的订单已完成，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单完成通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单完成通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        String eventId = "cancelled:" + event.getOrderId();
        if (!tryAcquireLock("OrderCancelled", eventId)) {
            return;
        }

        try {
            Long buyerId = getBuyerId(event.getOrderId());
            if (buyerId == null) {
                log.warn("订单不存在，跳过取消通知: orderId={}", event.getOrderId());
                return;
            }
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(buyerId)
                    .title("订单已取消")
                    .content("您的订单已取消，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单取消通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单取消通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) {
        String eventId = "refunded:" + event.getOrderId();
        if (!tryAcquireLock("OrderRefunded", eventId)) {
            return;
        }

        try {
            Long buyerId = getBuyerId(event.getOrderId());
            if (buyerId == null) {
                log.warn("订单不存在，跳过退款通知: orderId={}", event.getOrderId());
                return;
            }
            messageCommandHandler.handle(SendSystemMessageCommand.builder()
                    .receiverId(buyerId)
                    .title("订单已退款")
                    .content("您的订单已退款，订单号: " + event.getOrderId())
                    .businessId(event.getOrderId())
                    .build());
            log.info("订单退款通知已发送: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单退款通知发送失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    private boolean tryAcquireLock(String eventType, String eventId) {
        if (idempotencyChecker.isDuplicate(eventType, eventId)) {
            return false;
        }
        return idempotencyChecker.tryMark(eventType, eventId);
    }

    private Long getBuyerId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return orderReadRepository.findById(OrderId.of(orderId))
                .map(readModel -> readModel.buyerId())
                .orElse(null);
    }
}
