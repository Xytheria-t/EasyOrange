package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Token 吊销检查过滤器。
 * <p>
 * 在 JWT 认证完成后检查 Token 是否被列入 Redis 黑名单或被踢下线。
 * 密码学验证由 JwtDecoder 负责，本过滤器仅做吊销状态验证。
 */
@Component
@RequiredArgsConstructor
@NullMarked
public class TokenRevocationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof Jwt jwt) {
            try {
                checkRevocation(jwt);
                chain.doFilter(request, response);
            } catch (BadJwtException e) {
                SecurityContextHolder.clearContext();
                sendUnauthorized(response, e.getMessage());
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void checkRevocation(Jwt jwt) {
        String jti = jwt.getId();
        if (jti != null && Boolean.TRUE.equals(redis.hasKey(LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti))) {
            throw new BadJwtException("Token has been revoked");
        }

        var forceLogoutKey = LoginCacheConstants.FORCE_LOGOUT_KEY + jwt.getSubject();
        String forceLogoutTime = redis.opsForValue().get(forceLogoutKey);
        if (forceLogoutTime != null) {
            Instant iat = jwt.getIssuedAt();
            if (iat != null && iat.toEpochMilli() < Long.parseLong(forceLogoutTime)) {
                throw new BadJwtException("Token revoked by force logout");
            }
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Result.error(ResultCode.UNAUTHORIZED, message));
    }
}
