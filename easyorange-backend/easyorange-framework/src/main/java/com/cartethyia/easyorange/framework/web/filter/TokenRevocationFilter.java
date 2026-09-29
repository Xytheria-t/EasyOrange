package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token 吊销检查过滤器 — JWT 认证完成后查 Redis 黑名单与强制登出时间戳，密码学验证由 JwtDecoder 负责。
 * <p>
 * Redis 不可用时 <b>fail-open 放行</b>：签名与有效期已验过，本检查只是「已验签之后的附加拦截」，且黑名单 key
 * 的 TTL 只等于 token 剩余有效期，Redis 抖动造成的安全敞口限于该 token 自然过期前的一小段时间；换来的是
 * Redis 故障不把每个已认证请求打成 500。降级量由 {@code easyorange.security.revocation_check_degraded} 计数，
 * 非 0 即说明降级正在生效。
 * <p>
 * 由 {@code SecurityConfig} 局部装配（不加 {@code @Component}）：执行位置由 Security 链决定，被容器链再自动
 * 注册一次只会让顺序变成两套事实。
 */
@Slf4j
@NullMarked
public class TokenRevocationFilter extends OncePerRequestFilter {

    private static final String METRIC_DEGRADED = "easyorange.security.revocation_check_degraded";

    private final StringRedisTemplate redis;
    private final ErrorResponseWriter errorResponseWriter;
    private final MeterRegistry meterRegistry;

    public TokenRevocationFilter(
            StringRedisTemplate redis, ErrorResponseWriter errorResponseWriter, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.errorResponseWriter = errorResponseWriter;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof Jwt jwt) {
            try {
                checkRevocation(jwt);
            } catch (BadJwtException e) {
                SecurityContextHolder.clearContext();
                sendUnauthorized(response, e.getMessage());
                return;
            } catch (RuntimeException e) {
                // Redis 层异常不是鉴权失败，按 fail-open 放行并计数（见类注释的取舍）
                log.warn(
                        "action=revocation_check_degraded, jti={}, type={}",
                        jwt.getId(),
                        e.getClass().getName(),
                        e);
                meterRegistry.counter(METRIC_DEGRADED).increment();
            }
        }
        chain.doFilter(request, response);
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
        errorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED, message);
    }
}
