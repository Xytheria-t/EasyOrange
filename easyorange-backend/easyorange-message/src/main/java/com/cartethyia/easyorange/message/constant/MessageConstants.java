package com.cartethyia.easyorange.message.constant;

/**
 * 消息常量
 *
 * @author cartethyia
 * @date 2026/03/06
 */
public class MessageConstants {

    private MessageConstants() {
    }

    public static final int STATUS_ENABLED = 1;

    public static final int STATUS_DISABLED = 0;

    public static final int TEMPLATE_STATUS_ENABLED = 1;

    public static final int PUSH_STATUS_PENDING = 0;

    public static final int PUSH_STATUS_PUSHED = 1;

    public static final int PUSH_STATUS_FAILED = 2;

    public static final int DEFAULT_RETRY_COUNT = 0;

    public static final int DEFAULT_MAX_RETRY_COUNT = 3;

    public static final String WS_ENDPOINT = "/ws";

    public static final String WS_USER_PREFIX = "/user";

    public static final String WS_TOPIC_PREFIX = "/topic";

    public static final String WS_QUEUE_PREFIX = "/queue";

    public static final String WS_MESSAGE_DESTINATION = "/queue/message";

    public static final String WS_NOTIFICATION_DESTINATION = "/queue/notification";

    public static final int MAX_UNREAD_COUNT = 99;
}
