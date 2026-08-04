package com.cartethyia.easyorange.message.domain.port;

/**
 * Port for checking user online status and sending real-time notifications.
 * Implemented by the WebSocket infrastructure adapter.
 */
public interface MessageNotifierPort {
    boolean isUserOnline(String userId);

    void sendNotification(String userId, Object notification);
}
