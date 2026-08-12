package com.cartethyia.easyorange.message.adapter.outbound.websocket;

import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotifier implements MessageNotifierPort {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    @Override
    public void sendNotification(String userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification", notification);
        } catch (Exception e) {
            log.warn("action=send_websocket_notification_failed userId={}", userId, e);
        }
    }

    @Override
    public boolean isUserOnline(String userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }
}
