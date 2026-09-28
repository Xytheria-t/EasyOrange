package com.cartethyia.easyorange.framework.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.ObjectMapper;

/**
 * TokenRevocationFilter 吊销检查与 Redis 故障降级 — 单元测试。
 * <p>
 * 降级口径是 fail-open：Redis 抖动不能把每个已认证请求打成无日志无指标的 500，
 * 代价是「已验签 token 暂时绕过吊销检查」，窗口上限即该 token 的剩余有效期。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationFilter 吊销检查")
class TokenRevocationFilterTest {

    private static final String METRIC_DEGRADED = "easyorange.security.revocation_check_degraded";

    @Mock
    private StringRedisTemplate redis;

    private SimpleMeterRegistry meterRegistry;
    private TokenRevocationFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new TokenRevocationFilter(redis, new ErrorResponseWriter(new ObjectMapper()), meterRegistry);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWith(Jwt jwt) {
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        auth.setDetails(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Jwt jwtWith(String jti) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("1001")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .claim("authorities", List.of("ROLE_USER"))
                .build();
    }

    @Test
    @DisplayName("未进入吊销检查路径：无 JWT 认证上下文直接透传")
    void noAuthentication_passesThrough() throws Exception {
        var invoked = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("jti 在黑名单：清空 SecurityContext 并返回 401，不进入下游")
    void blacklistedJti_returnsUnauthorized() throws Exception {
        when(redis.hasKey(LoginCacheConstants.TOKEN_BLACKLIST_KEY + "jti-1")).thenReturn(true);
        authenticateWith(jwtWith("jti-1"));
        var invoked = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (r, s) -> invoked.set(true));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Redis 连接失败：fail-open 放行并计降级指标（不得变成 500）")
    void redisUnavailable_failsOpenAndCounts() throws Exception {
        when(redis.hasKey(anyString())).thenThrow(new IllegalStateException("redis down"));
        authenticateWith(jwtWith("jti-2"));
        var invoked = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(meterRegistry.counter(METRIC_DEGRADED).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("强制作废时间晚于签发时间：返回 401")
    void forceLogoutAfterIssuedAt_returnsUnauthorized() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(LoginCacheConstants.FORCE_LOGOUT_KEY + "1001"))
                .thenReturn(String.valueOf(Instant.now().plusSeconds(600).toEpochMilli()));
        authenticateWith(jwtWith("jti-3"));
        var invoked = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (r, s) -> invoked.set(true));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("未吊销：正常放行，不计降级")
    void notRevoked_passesThrough() throws Exception {
        when(redis.hasKey(LoginCacheConstants.TOKEN_BLACKLIST_KEY + "jti-4")).thenReturn(false);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        authenticateWith(jwtWith("jti-4"));
        var invoked = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (r, s) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(meterRegistry.find(METRIC_DEGRADED).counter()).isNull();
        verify(valueOps).get(anyString());
    }
}
