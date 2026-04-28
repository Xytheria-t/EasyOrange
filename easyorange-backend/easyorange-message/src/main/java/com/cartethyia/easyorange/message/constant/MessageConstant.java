package com.cartethyia.easyorange.message.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageConstant {

    // ==================== 通用状态码 ====================
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    // ==================== 消息模板状态 ====================
    public static final int TEMPLATE_STATUS_ENABLED = 1;

    // ==================== 推送状态 ====================
    public static final int PUSH_STATUS_PENDING = 0;
    public static final int PUSH_STATUS_PUSHED = 1;
    public static final int PUSH_STATUS_FAILED = 2;

    // ==================== 重试常量 ====================
    public static final int DEFAULT_RETRY_COUNT = 0;
    public static final int DEFAULT_MAX_RETRY_COUNT = 3;

    // ==================== WebSocket 常量 ====================
    public static final String WS_ENDPOINT = "/ws";
    public static final String WS_USER_PREFIX = "/user";
    public static final String WS_TOPIC_PREFIX = "/topic";
    public static final String WS_QUEUE_PREFIX = "/queue";
    public static final String WS_MESSAGE_DESTINATION = "/queue/message";
    public static final String WS_NOTIFICATION_DESTINATION = "/queue/notification";

    // ==================== 未读消息上限 ====================
    public static final int MAX_UNREAD_COUNT = 99;
}
