package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketEventListener(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("action=websocket_connected sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("action=websocket_disconnected sessionId={}", sessionId);
    }

    @EventListener
    public void handleMessageRecalledEvent(MessageRecalledEvent event) {
        chatWebSocketHandler.broadcastRecallEvent(
                event.getConversationId(),
                event.getMessageId(),
                event.getOperatorId()
        );
    }
}
