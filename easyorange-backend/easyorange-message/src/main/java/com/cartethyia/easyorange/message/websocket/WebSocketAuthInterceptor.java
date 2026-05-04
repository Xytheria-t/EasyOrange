package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.framework.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

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
                if (jwtUtil.validateToken(token)) {
                    String subject = jwtUtil.getSubject(token).orElse(null);
                    if (subject != null) {
                        Long userId = Long.parseLong(subject);
                        attributes.put("userId", userId);
                        attributes.put("username", subject);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("WebSocket握手失败: token验证失败, error={}", e.getMessage());
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
