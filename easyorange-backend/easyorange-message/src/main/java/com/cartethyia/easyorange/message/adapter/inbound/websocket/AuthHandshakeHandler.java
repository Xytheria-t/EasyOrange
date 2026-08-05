package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 将握手拦截器认证出的 userId 提升为 WebSocket 会话 {@link Principal}，
 * 使 STOMP {@code @MessageMapping} 处理器可直接通过 {@code Principal.getName()} 取当前用户
 * （STOMP 线程上 SecurityContextHolder 不可用，不能依赖 SecurityContextUtil）。
 */
public class AuthHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userId = attributes.get(WebSocketAttributes.USER_ID);
        if (userId != null) {
            return () -> userId.toString();
        }
        return null;
    }
}
