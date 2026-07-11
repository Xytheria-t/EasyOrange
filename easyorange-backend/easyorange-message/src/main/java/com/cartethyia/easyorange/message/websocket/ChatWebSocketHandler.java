package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.application.service.RateLimiterService;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageCommandHandler messageCommandHandler;
    private final TypingIndicatorService typingService;
    private final RateLimiterService rateLimiterService;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload WsMessage payload, Principal principal) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (!rateLimiterService.allowSendMessage(userId)) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/error",
                    Map.of("type", "RATE_LIMITED", "message", "发送过于频繁，请稍后再试")
            );
            return;
        }

        SendMessageCommand command = SendMessageCommand.builder()
                .receiverId(payload.getReceiverId())
                .type(payload.getType() != null ? payload.getType() : 0)
                .title(payload.getTitle() != null ? payload.getTitle() : "")
                .content(payload.getContent())
                .businessId(payload.getBusinessId())
                .conversationId(payload.getConversationId())
                .build();
        messageCommandHandler.handle(command);

        String dest = "/queue/chat/" + payload.getConversationId();
        messagingTemplate.convertAndSend(dest, payload);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.getReceiverId()),
                "/queue/unread-count",
                Map.of("conversationId", payload.getConversationId(), "increment", 1, "timestamp", Instant.now().toEpochMilli())
        );

        log.info("action=chat_message_sent conversationId={} senderId={} receiverId={}",
                payload.getConversationId(), userId, payload.getReceiverId());
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload WsMessage payload, Principal principal) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        typingService.setTyping(payload.getConversationId(), userId);

        messagingTemplate.convertAndSend(
                "/topic/chat/" + payload.getConversationId() + "/typing",
                (Object) Map.of("userId", userId, "timestamp", Instant.now().toEpochMilli())
        );

        log.debug("action=typing_indicator conversationId={} userId={}", payload.getConversationId(), userId);
    }

    public void broadcastRecallEvent(String conversationId, String messageId, String operatorId) {
        String recallDest = "/topic/chat/" + conversationId + "/recall";
        Map<String, Object> recallPayload = Map.of(
                "messageId", String.valueOf(messageId),
                "conversationId", conversationId,
                "operatorId", String.valueOf(operatorId),
                "recalledAt", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(recallDest, (Object) recallPayload);

        log.info("action=recall_broadcast conversationId={} messageId={} operatorId={}",
                conversationId, messageId, operatorId);
    }

    public void broadcastUnreadUpdate(String targetUserId, String conversationId, int count) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(targetUserId),
                "/queue/unread-count",
                Map.of(
                        "conversationId", conversationId,
                        "count", count,
                        "timestamp", Instant.now().toEpochMilli()
                )
        );
    }
}
