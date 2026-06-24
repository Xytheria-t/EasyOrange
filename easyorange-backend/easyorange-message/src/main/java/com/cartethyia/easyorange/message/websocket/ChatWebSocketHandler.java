package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.port.OfferProcessingPort;
import com.cartethyia.easyorange.message.domain.service.RateLimiterService;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.OfferCommand;
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
    private final WebSocketNotifier webSocketNotifier;
    private final OfferProcessingPort offerProcessingPort;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload WsMessage payload, Principal principal) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

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
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        typingService.setTyping(payload.getConversationId(), userId);

        messagingTemplate.convertAndSend(
                "/topic/chat/" + payload.getConversationId() + "/typing",
                (Object) Map.of("userId", userId, "timestamp", Instant.now().toEpochMilli())
        );

        log.debug("action=typing_indicator conversationId={} userId={}", payload.getConversationId(), userId);
    }

    @MessageMapping("/offer.make")
    public void handleOffer(@Payload OfferCommand command, Principal principal) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();
        log.info("action=offer_made buyerId={} productId={} offerPrice={}",
                buyerId, command.getProductId(), command.getOfferPrice());

        OfferProcessingPort.OfferResult result = offerProcessingPort.processOffer(
                buyerId, command.getProductId(), command.getOfferPrice());

        // 推送议价结果给买家
        switch (result.decisionType()) {
            case "ACCEPT" -> webSocketNotifier.notifyOfferAccepted(
                    buyerId, command.getProductId(), command.getOfferPrice());
            case "REJECT" -> webSocketNotifier.notifyOfferRejected(
                    buyerId, command.getProductId(), command.getOfferPrice());
            case "COUNTER" -> webSocketNotifier.notifyCounterOffer(
                    buyerId, command.getProductId(), result.counterPrice());
            default -> log.warn("action=unknown_offer_decision_type decisionType={}", result.decisionType());
        }
    }

    public void broadcastRecallEvent(String conversationId, Long messageId, Long operatorId) {
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
