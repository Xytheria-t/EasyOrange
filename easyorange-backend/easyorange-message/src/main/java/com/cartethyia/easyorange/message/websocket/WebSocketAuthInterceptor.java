package com.cartethyia.easyorange.message.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = extractToken(servletRequest);
            if (token == null) {
                log.warn("WebSocket握手失败: 未提供token");
                return false;
            }

            try {
                Jwt jwt = jwtDecoder.decode(token);
                Long userId = Long.valueOf(jwt.getSubject());
                attributes.put("userId", userId);
                attributes.put("username", jwt.getSubject());
                return true;
            } catch (JwtException e) {
                log.warn("WebSocket握手失败: token验证失败", e);
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // WebSocket 握手后无需特殊处理
    }

    private String extractToken(ServletServerHttpRequest request) {
        String token = request.getServletRequest().getParameter("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        String authHeader = request.getServletRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
