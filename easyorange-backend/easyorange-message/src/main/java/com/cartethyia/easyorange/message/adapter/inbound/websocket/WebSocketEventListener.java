package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OfflineMessageStoreService offlineMessageStoreService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("action=websocket_connected sessionId={}", sessionId);

        // 上线补推离线站内信（此时 SimpUserRegistry 已注册用户，在线判定成立）；重推失败不影响连接建立
        Principal user = headerAccessor.getUser();
        if (user != null && user.getName() != null) {
            try {
                offlineMessageStoreService.replayPending(user.getName());
            } catch (Exception e) {
                log.warn("action=offline_message_replay_failed userId={}", user.getName(), e);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("action=websocket_disconnected sessionId={}", sessionId);
    }
}
