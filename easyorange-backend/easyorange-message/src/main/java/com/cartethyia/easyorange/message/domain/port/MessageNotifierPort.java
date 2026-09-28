package com.cartethyia.easyorange.message.domain.port;

/**
 * 实时投递端口 —— 在线判定与站内信推送，域内不感知 STOMP / SimpUserRegistry。
 * <p>
 * 取舍：只暴露「推送站内信」与「查在线」两件事，聊天消息的回显走 inbound WebSocket 适配器自己的
 * SimpMessagingTemplate（它要在同一个会话里按房间广播），故不在本端口里。
 * <p>
 * 边界：实现在 WebSocket 适配器内，发送失败只告警不抛出——此时消息行已提交，
 * 可靠路径是离线补推而不是让发送方看到失败。
 */
public interface MessageNotifierPort {

    boolean isUserOnline(String userId);

    void sendNotification(String userId, Object notification);
}
