package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.dto.request.WsMessage;
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
            log.error("action=send_websocket_message_failed userId={} error={}", userId, e.getMessage());
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
            log.error("action=send_websocket_notification_failed userId={} error={}", userId, e.getMessage());
        }
    }

    public void broadcast(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("action=broadcast_websocket_message_failed destination={} error={}", destination, e.getMessage());
        }
    }

    public boolean isUserOnline(Long userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }
}
