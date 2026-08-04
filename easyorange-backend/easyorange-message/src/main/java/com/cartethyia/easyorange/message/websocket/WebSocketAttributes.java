package com.cartethyia.easyorange.message.websocket;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** WebSocket 握手会话属性 key，供握手拦截器写入、AuthHandshakeHandler 读取。 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebSocketAttributes {

    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
}
