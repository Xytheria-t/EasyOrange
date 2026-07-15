package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;
    private final StringRedisTemplate redis;

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

                // 检查 token 是否已被吊销
                String jti = jwt.getId();
                if (jti != null && Boolean.TRUE.equals(
                        redis.hasKey(LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti))) {
                    log.warn("WebSocket握手失败: token已被吊销");
                    return false;
                }
                var forceLogoutKey = LoginCacheConstants.FORCE_LOGOUT_KEY + jwt.getSubject();
                String forceLogoutTime = redis.opsForValue().get(forceLogoutKey);
                if (forceLogoutTime != null) {
                    Instant iat = jwt.getIssuedAt();
                    if (iat != null && iat.toEpochMilli() < Long.parseLong(forceLogoutTime)) {
                        log.warn("WebSocket握手失败: 用户已被强制下线");
                        return false;
                    }
                }

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
