package com.cartethyia.easyorange.message.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageConstant {

    // ── WebSocket 常量 ──
    public static final String WS_ENDPOINT = "/ws";
    public static final String WS_USER_PREFIX = "/user";
    public static final String WS_TOPIC_PREFIX = "/topic";
    public static final String WS_QUEUE_PREFIX = "/queue";
}
