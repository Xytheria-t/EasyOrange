package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageCommandHandler messageCommandHandler;
    private final TypingIndicatorService typingService;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload WsMessage payload, Principal principal) {
        String userId = requireUserId(principal);

        // 限流唯一裁决点在 MessageCommandHandler（REST 与 WS 共用），此处不再前置扣减，
        // 避免每条 WS 消息被双重计数导致有效限流阈值减半。超限由 @MessageExceptionHandler 映射错误帧。
        SendMessageCommand command = new SendMessageCommand(
                payload.getReceiverId(),
                payload.getType(),
                payload.getTitle() != null ? payload.getTitle() : "",
                payload.getContent(),
                payload.getBusinessId(),
                payload.getConversationId());
        messageCommandHandler.handle(command);

        String dest = "/queue/chat/" + payload.getConversationId();
        messagingTemplate.convertAndSend(dest, payload);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.getReceiverId()),
                "/queue/unread-count",
                Map.of(
                        "conversationId",
                        payload.getConversationId(),
                        "increment",
                        1,
                        "timestamp",
                        Instant.now().toEpochMilli()));

        log.info(
                "action=chat_message_sent conversationId={} senderId={} receiverId={}",
                payload.getConversationId(),
                userId,
                payload.getReceiverId());
    }

    /** 消息命令异常（当前为限流）统一映射为发送方错误帧，避免在 STOMP 线程上抛出未捕获异常。 */
    @MessageExceptionHandler(MessageDomainException.class)
    public void handleDomainException(MessageDomainException ex, Principal principal) {
        log.warn(
                "action=chat_send_rejected error={} user={}",
                ex.getMessage(),
                principal != null ? principal.getName() : null);
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(principal.getName()),
                    "/queue/error",
                    Map.of("type", "RATE_LIMITED", "message", ex.getMessage()));
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload WsMessage payload, Principal principal) {
        String userId = requireUserId(principal);

        typingService.setTyping(payload.getConversationId(), userId);

        messagingTemplate.convertAndSend("/topic/chat/" + payload.getConversationId() + "/typing", (Object)
                Map.of("userId", userId, "timestamp", Instant.now().toEpochMilli()));

        log.debug("action=typing_indicator conversationId={} userId={}", payload.getConversationId(), userId);
    }

    public void broadcastRecallEvent(String conversationId, String messageId, String operatorId) {
        String recallDest = "/topic/chat/" + conversationId + "/recall";
        Map<String, Object> recallPayload = Map.of(
                "messageId", String.valueOf(messageId),
                "conversationId", conversationId,
                "operatorId", String.valueOf(operatorId),
                "recalledAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(recallDest, (Object) recallPayload);

        log.info(
                "action=recall_broadcast conversationId={} messageId={} operatorId={}",
                conversationId,
                messageId,
                operatorId);
    }

    public void broadcastUnreadUpdate(String targetUserId, String conversationId, int count) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(targetUserId),
                "/queue/unread-count",
                Map.of(
                        "conversationId", conversationId,
                        "count", count,
                        "timestamp", Instant.now().toEpochMilli()));
    }

    /** 从握手认证建立的 Principal 取当前用户 ID（STOMP 线程上 SecurityContextHolder 不可用）。 */
    private static String requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new MessageDomainException("未认证的用户");
        }
        return principal.getName();
    }
}
