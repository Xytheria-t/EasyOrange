package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public void sendMessage(Long userId, WsMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/message",
                    message
            );
        } catch (Exception e) {
            log.warn("action=send_websocket_message_failed userId={}", userId, e);
        }
    }

    public void sendNotification(Long userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notification",
                    notification
            );
        } catch (Exception e) {
            log.warn("action=send_websocket_notification_failed userId={}", userId, e);
        }
    }

    public void broadcast(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.warn("action=broadcast_websocket_message_failed destination={}", destination, e);
        }
    }

    public boolean isUserOnline(Long userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }
}
