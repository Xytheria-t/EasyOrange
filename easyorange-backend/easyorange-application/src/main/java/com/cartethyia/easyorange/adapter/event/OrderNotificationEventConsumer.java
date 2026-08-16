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
import com.cartethyia.easyorange.order.domain.event.OrderEvent;
import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.order.domain.event.OrderRefundedEvent;
import com.cartethyia.easyorange.order.domain.event.OrderShippedEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单通知消费者 — 将订单生命周期事件转为站内消息通知买家。
 * <p>
 * 监听 6 种订单事件，按事件类型映射为对应的系统消息标题/内容（见 {@link NotificationTemplate}）。
 * 事件载荷自包含 buyerId（聚合根发布时携带），消费者无需回查订单。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_NOTIFICATION, containerFactory = "domainEventContainerFactory")
public class OrderNotificationEventConsumer {

    private final EventConsumerHandler handler;
    private final MessageCommandHandler messageCommandHandler;

    public OrderNotificationEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            MessageCommandHandler messageCommandHandler) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.messageCommandHandler = messageCommandHandler;
    }

    @RabbitHandler
    public void onOrderEvent(OrderEvent event, Message message) {
        handler.handle(event, message, () -> {
            var template = NotificationTemplate.of(event.getClass());
            var buyerId = event.buyerId();
            if (buyerId == null || buyerId.isBlank()) {
                // 滚动部署窗口兼容：旧版消息无 buyerId 字段，降级跳过并告警，不落脏数据
                log.warn(
                        "action=skip_notification reason=missing_buyer_id orderId={} title={}",
                        event.orderId(),
                        template.title());
                return;
            }
            messageCommandHandler.handle(new SendSystemMessageCommand(
                    buyerId, template.title(), template.content(event.orderId()), event.orderId()));
        });
    }

    /** 订单事件类型 → 站内消息模板；内容为含订单号占位的格式串。 */
    private enum NotificationTemplate {
        CREATED(OrderCreatedEvent.class, "订单已创建", "您的订单已创建，订单号: %s"),
        PAID(OrderPaidEvent.class, "订单已支付", "您的订单已支付成功，订单号: %s"),
        SHIPPED(OrderShippedEvent.class, "订单已发货", "您的订单已发货，订单号: %s"),
        COMPLETED(OrderCompletedEvent.class, "订单已完成", "您的订单已完成，订单号: %s"),
        CANCELLED(OrderCancelledEvent.class, "订单已取消", "您的订单已取消，订单号: %s"),
        REFUNDED(OrderRefundedEvent.class, "订单已退款", "您的订单已退款，订单号: %s");

        private static final Map<Class<?>, NotificationTemplate> BY_EVENT_TYPE =
                Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(t -> t.eventType, t -> t));

        private final Class<? extends OrderEvent> eventType;
        private final String title;
        private final String contentTemplate;

        NotificationTemplate(Class<? extends OrderEvent> eventType, String title, String contentTemplate) {
            this.eventType = eventType;
            this.title = title;
            this.contentTemplate = contentTemplate;
        }

        static NotificationTemplate of(Class<?> eventType) {
            var template = BY_EVENT_TYPE.get(eventType);
            if (template == null) {
                throw new IllegalArgumentException("unsupported order event type: " + eventType.getName());
            }
            return template;
        }

        String title() {
            return title;
        }

        String content(String orderId) {
            return contentTemplate.formatted(orderId);
        }
    }
}
