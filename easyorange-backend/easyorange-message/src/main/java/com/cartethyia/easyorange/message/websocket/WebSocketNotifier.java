package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.dto.request.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket 消息通知器
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户 ID
     * @param message 消息内容
     */
    public void sendMessage(Long userId, WsMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/message",
                    message
            );
            log.debug("WebSocket 发送消息成功，用户 ID：{}", userId);
        } catch (Exception e) {
            log.error("action=send_websocket_message_failed userId={} error={}", userId, e.getMessage());
        }
    }

    /**
     * 发送通知给指定用户
     *
     * @param userId  用户 ID
     * @param notification 通知内容
     */
    public void sendNotification(Long userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notification",
                    notification
            );
            log.debug("WebSocket 发送通知成功，用户 ID：{}", userId);
        } catch (Exception e) {
            log.error("action=send_websocket_notification_failed userId={} error={}", userId, e.getMessage());
        }
    }

    /**
     * 广播消息给所有用户
     *
     * @param destination 目的地
     * @param message     消息内容
     */
    public void broadcast(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("WebSocket 广播消息成功，目的地：{}", destination);
        } catch (Exception e) {
            log.error("action=broadcast_websocket_message_failed destination={} error={}", destination, e.getMessage());
        }
    }
}
