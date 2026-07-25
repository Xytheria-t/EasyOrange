package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
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
 * 幂等键默认 {@code OrderCreated:orderId:v1}，由基类自动处理。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_NOTIFICATION, containerFactory = "domainEventContainerFactory")
public class OrderNotificationEventConsumer extends AbstractDomainEventConsumer {

    private final MessageCommandHandler messageCommandHandler;
    private final OrderReadRepository orderReadRepository;

    public OrderNotificationEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                          EventMetricsService metricsService,
                                          MessageCommandHandler messageCommandHandler,
                                          OrderReadRepository orderReadRepository) {
        super(idempotencyChecker, metricsService);
        this.messageCommandHandler = messageCommandHandler;
        this.orderReadRepository = orderReadRepository;
    }

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onOrderPaid(OrderPaidEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onOrderShipped(OrderShippedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event, Message message) {
        handle(event, message);
    }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case OrderCreatedEvent e -> sendNotification(e.buyerId(), "订单已创建",
                    "您的订单已创建，订单号: " + e.orderId(), e.orderId());
            case OrderPaidEvent e -> sendToBuyer(e.orderId(), "订单已支付",
                    "您的订单已支付成功，订单号: " + e.orderId());
            case OrderShippedEvent e -> sendToBuyer(e.orderId(), "订单已发货",
                    "您的订单已发货，订单号: " + e.orderId());
            case OrderCompletedEvent e -> sendToBuyer(e.orderId(), "订单已完成",
                    "您的订单已完成，订单号: " + e.orderId());
            case OrderCancelledEvent e -> sendToBuyer(e.orderId(), "订单已取消",
                    "您的订单已取消，订单号: " + e.orderId());
            case OrderRefundedEvent e -> sendToBuyer(e.orderId(), "订单已退款",
                    "您的订单已退款，订单号: " + e.orderId());
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    private void sendToBuyer(String orderId, String title, String content) {
        String buyerId = lookupBuyerId(orderId);
        if (buyerId == null) {
            return;
        }
        sendNotification(buyerId, title, content, orderId);
    }

    private void sendNotification(String receiverId, String title, String content, String businessId) {
        messageCommandHandler.handle(new SendSystemMessageCommand(
                receiverId,
                title,
                content,
                businessId
        ));
    }

    private String lookupBuyerId(String orderId) {
        if (orderId == null) {
            return null;
        }
        return orderReadRepository.findById(OrderId.of(orderId))
                .map(readModel -> readModel.buyerId())
                .orElse(null);
    }
}
