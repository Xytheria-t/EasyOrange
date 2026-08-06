package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 消息撤回 WebSocket 广播消费者 — 收到撤回事件后向所有在线连接广播。
 * <p>
 * 关闭幂等检查：WebSocket 广播是幂等的（重复广播对客户端无副作用）。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET, containerFactory = "domainEventContainerFactory")
public class WebSocketEventConsumer {

    private final EventConsumerHandler handler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            ChatWebSocketHandler chatWebSocketHandler) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService, false);
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @RabbitHandler
    public void onMessageRecalled(MessageRecalledEvent event, Message message) {
        handler.handle(
                event,
                message,
                metadata -> chatWebSocketHandler.broadcastRecallEvent(
                        event.conversationId(), event.messageId(), event.operatorId()));
    }
}
