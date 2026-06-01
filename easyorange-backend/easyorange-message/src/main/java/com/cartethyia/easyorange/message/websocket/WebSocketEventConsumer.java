package com.cartethyia.easyorange.message.websocket;

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
        queues = "eo.message.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void handleMessageRecalledEvent(MessageRecalledEvent event) {
        log.info("收到消息撤回事件: conversationId={}, messageId={}, operatorId={}",
                event.getConversationId(), event.getMessageId(), event.getOperatorId());

        try {
            chatWebSocketHandler.broadcastRecallEvent(
                    event.getConversationId(),
                    event.getMessageId(),
                    event.getOperatorId()
            );

            log.info("消息撤回广播成功: messageId={}", event.getMessageId());
        } catch (Exception e) {
            log.error("消息撤回广播失败: messageId={}", event.getMessageId(), e);
            throw e;
        }
    }
}
