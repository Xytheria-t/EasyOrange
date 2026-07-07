package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketEventConsumer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET,
        containerFactory = "domainEventContainerFactory"
    )
    public void handleMessageRecalledEvent(MessageRecalledEvent event) {
        log.info("收到消息撤回事件: conversationId={}, messageId={}, operatorId={}",
                event.conversationId(), event.messageId(), event.operatorId());

        try {
            chatWebSocketHandler.broadcastRecallEvent(
                    event.conversationId(),
                    event.messageId(),
                    event.operatorId()
            );

            log.info("消息撤回广播成功: messageId={}", event.messageId());
        } catch (Exception e) {
            log.error("消息撤回广播失败: messageId={}", event.messageId(), e);
            throw e;
        }
    }
}
