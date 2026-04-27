package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.dto.request.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息处理器
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageHandler {

    private final WebSocketNotifier webSocketNotifier;

    /**
     * 处理客户端发送的消息
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload WsMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Long receiverId = message.getReceiverId();
        if (receiverId != null) {
            webSocketNotifier.sendMessage(receiverId, message);
        }
    }

    /**
     * 处理用户加入聊天
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload WsMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromSession(headerAccessor);
        String username = getUsernameFromSession(headerAccessor);

        if (userId != null) {
            headerAccessor.getSessionAttributes().put("userId", userId);
            headerAccessor.getSessionAttributes().put("username", username);
            log.info("action=websocket_join userId={} username={}", userId, username);
        } else {
            log.warn("action=websocket_join_failed reason=cannot_get_user_info");
        }
    }

    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object userId = headerAccessor.getSessionAttributes().get("userId");
        if (userId instanceof Long typedUserId) {
            return typedUserId;
        }
        return null;
    }

    private String getUsernameFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object username = headerAccessor.getSessionAttributes().get("username");
        if (username instanceof String typedUsername) {
            return typedUsername;
        }
        return "未知用户";
    }
}
