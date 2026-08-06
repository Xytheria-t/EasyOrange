package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单通知消费者 — 将订单生命周期事件转为站内消息通知买家。
 * <p>
 * 监听 6 种订单事件，每个事件按状态映射为对应的系统消息标题/内容。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_NOTIFICATION, containerFactory = "domainEventContainerFactory")
public class OrderNotificationEventConsumer {

    private final EventConsumerHandler handler;
    private final MessageCommandHandler messageCommandHandler;
    private final OrderReadRepository orderReadRepository;

    public OrderNotificationEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            MessageCommandHandler messageCommandHandler,
            OrderReadRepository orderReadRepository) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.messageCommandHandler = messageCommandHandler;
        this.orderReadRepository = orderReadRepository;
    }

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event, Message message) {
        handler.handle(
                event,
                message,
                metadata ->
                        sendNotification(event.buyerId(), "订单已创建", "您的订单已创建，订单号: " + event.orderId(), event.orderId()));
    }

    @RabbitHandler
    public void onOrderPaid(OrderPaidEvent event, Message message) {
        handler.handle(
                event, message, metadata -> sendToBuyer(event.orderId(), "订单已支付", "您的订单已支付成功，订单号: " + event.orderId()));
    }

    @RabbitHandler
    public void onOrderShipped(OrderShippedEvent event, Message message) {
        handler.handle(
                event, message, metadata -> sendToBuyer(event.orderId(), "订单已发货", "您的订单已发货，订单号: " + event.orderId()));
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) {
        handler.handle(
                event, message, metadata -> sendToBuyer(event.orderId(), "订单已完成", "您的订单已完成，订单号: " + event.orderId()));
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event, Message message) {
        handler.handle(
                event, message, metadata -> sendToBuyer(event.orderId(), "订单已取消", "您的订单已取消，订单号: " + event.orderId()));
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event, Message message) {
        handler.handle(
                event, message, metadata -> sendToBuyer(event.orderId(), "订单已退款", "您的订单已退款，订单号: " + event.orderId()));
    }

    private void sendToBuyer(String orderId, String title, String content) {
        var buyerId = lookupBuyerId(orderId);
        if (buyerId != null) {
            sendNotification(buyerId, title, content, orderId);
        }
    }

    private void sendNotification(String receiverId, String title, String content, String businessId) {
        messageCommandHandler.handle(new SendSystemMessageCommand(receiverId, title, content, businessId));
    }

    private String lookupBuyerId(String orderId) {
        if (orderId == null) return null;
        return orderReadRepository
                .findById(OrderId.of(orderId))
                .map(readModel -> readModel.buyerId())
                .orElse(null);
    }
}
