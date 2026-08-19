package com.cartethyia.easyorange.framework.auth.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * TokenService 门面 — 单元测试。
 * <p>
 * 验证：access JWT 签发（type=access）/ 黑名单吊销、refresh opaque 签发 / 轮换（含复用检测）/ 吊销 / 按用户吊销。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 门面")
class TokenServiceImplTest {

    private static final String USER_ID = "u1";
    private static final String SESSION = LoginCacheConstants.REFRESH_SESSION_KEY;
    private static final String USED = LoginCacheConstants.REFRESH_USED_KEY;
    private static final String USER_KEYPREFIX = LoginCacheConstants.REFRESH_USER_KEY;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtProperties jwtProperties;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        // valueOps / setOps / jwtProperties 桩非全部用例使用，标记 lenient
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
        lenient().when(jwtProperties.getIssuer()).thenReturn("easyorange");
        lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(30L);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(7L);
        tokenService = new TokenServiceImpl(stringRedisTemplate, jwtEncoder, jwtDecoder, jwtProperties);
    }

    // ==================== access token ====================

    @Test
    @DisplayName("createAccessToken 签发 type=access 的 JWT")
    void createAccessToken_buildsAccessJwt() {
        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        var encoded = mockJwt("encoded-access");
        when(jwtEncoder.encode(captor.capture())).thenReturn(encoded);

        var token = tokenService.createAccessToken(USER_ID, "alice", List.of("ROLE_USER"));

        assertThat(token).isEqualTo("encoded-access");
        var claims = captor.getValue().getClaims();
        assertThat(claims.<String>getClaim("type")).isEqualTo("access");
        assertThat(claims.<String>getClaim("sub")).isEqualTo(USER_ID);
        assertThat(claims.<String>getClaim("username")).isEqualTo("alice");
        assertThat(claims.<List<String>>getClaim("authorities")).isEqualTo(List.of("ROLE_USER"));
        assertThat(claims.<String>getClaim("iss")).isEqualTo("easyorange");
    }

    @Test
    @DisplayName("revokeAccessToken 将 access token 的 jti 加入黑名单")
    void revokeAccessToken_blacklistsJti() {
        var jwt = mockJwt("access-token");
        when(jwt.getId()).thenReturn("jti-1");
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(300));
        when(jwtDecoder.decode("access-token")).thenReturn(jwt);

        tokenService.revokeAccessToken("access-token");

        verify(valueOps)
                .set(
                        eq(LoginCacheConstants.TOKEN_BLACKLIST_KEY + "jti-1"), eq("1"),
                        anyLong(), eq(TimeUnit.SECONDS));
    }

    // ==================== refresh token：create ====================

    @Test
    @DisplayName("createRefreshToken 写入 SESSION 并登记到 USER 集合")
    void createRefreshToken_storesSessionAndRegistersUser() {
        var token = tokenService.createRefreshToken(USER_ID);

        assertThat(token).isNotBlank();
        var hash = sha256(token);
        verify(valueOps).set(eq(SESSION + hash), eq(USER_ID), eq(7L * 24 * 3600), eq(TimeUnit.SECONDS));
        verify(setOps).add(USER_KEYPREFIX + USER_ID, hash);
    }

    @Test
    @DisplayName("createRefreshToken 每次生成不同 token")
    void createRefreshToken_generatesUniqueTokens() {
        assertThat(tokenService.createRefreshToken(USER_ID)).isNotEqualTo(tokenService.createRefreshToken(USER_ID));
    }

    // ==================== refresh token：rotate ====================

    @Test
    @DisplayName("rotateRefreshToken 消费旧 token：删 SESSION、置 USED、发新 token 并迁移 USER 集合")
    void rotateRefreshToken_consumesOldAndIssuesNew() {
        var oldToken = "old-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(USER_ID);

        var rotation = tokenService.rotateRefreshToken(oldToken);

        assertThat(rotation.userId()).isEqualTo(USER_ID);
        assertThat(rotation.newToken()).isNotBlank();
        verify(stringRedisTemplate).delete(SESSION + oldHash);
        verify(valueOps).set(eq(USED + oldHash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        var newHash = sha256(rotation.newToken());
        verify(valueOps).set(eq(SESSION + newHash), eq(USER_ID), any(Long.class), eq(TimeUnit.SECONDS));
        verify(setOps).remove(USER_KEYPREFIX + USER_ID, oldHash);
        verify(setOps).add(USER_KEYPREFIX + USER_ID, newHash);
    }

    @Test
    @DisplayName("rotateRefreshToken 复用超宽限：吊销该用户全部会话并抛 401")
    void rotateRefreshToken_reuseBeyondGrace_revokesAllAndThrows() {
        var oldToken = "used-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(null);
        var staleTs = System.currentTimeMillis() - 60_000;
        when(valueOps.get(USED + oldHash)).thenReturn(USER_ID + ":" + staleTs);
        var otherHash = "other-active-hash";
        when(setOps.members(USER_KEYPREFIX + USER_ID)).thenReturn(Set.of(oldHash, otherHash));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(oldToken)).isInstanceOf(BusinessException.class);

        // 吊销所有会话
        verify(stringRedisTemplate).delete(SESSION + otherHash);
        verify(valueOps).set(eq(USED + otherHash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).delete(USER_KEYPREFIX + USER_ID);
    }

    @Test
    @DisplayName("rotateRefreshToken 复用未超宽限：仅抛 401，不吊销（多标签页并发）")
    void rotateRefreshToken_reuseWithinGrace_throwsWithoutRevoke() {
        var oldToken = "concurrent-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(null);
        var freshTs = System.currentTimeMillis();
        when(valueOps.get(USED + oldHash)).thenReturn(USER_ID + ":" + freshTs);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(oldToken)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(setOps);
    }

    @Test
    @DisplayName("rotateRefreshToken 未知/过期 token 抛 401")
    void rotateRefreshToken_unknownToken_throws() {
        var token = "expired-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(null);
        when(valueOps.get(USED + hash)).thenReturn(null);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(token)).isInstanceOf(BusinessException.class);
    }

    // ==================== refresh token：revoke ====================

    @Test
    @DisplayName("revokeRefreshToken 删除 SESSION、置 USED、从 USER 集合移除")
    void revokeRefreshToken_removesSessionAndMarksUsed() {
        var token = "revoke-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(USER_ID);

        tokenService.revokeRefreshToken(token);

        verify(stringRedisTemplate).delete(SESSION + hash);
        verify(valueOps).set(eq(USED + hash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(setOps).remove(USER_KEYPREFIX + USER_ID, hash);
    }

    @Test
    @DisplayName("revokeRefreshToken 已失效 token 为幂等，不抛错")
    void revokeRefreshToken_alreadyGone_isNoOp() {
        var token = "gone-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(null);

        tokenService.revokeRefreshToken(token);

        verify(stringRedisTemplate, never()).delete(SESSION + hash);
    }

    @Test
    @DisplayName("revokeAllUserSessions 吊销全部会话并设置 access 强制下线标记")
    void revokeAllUserSessions_revokesAllAndFlagsForceLogout() {
        var h1 = "h1";
        var h2 = "h2";
        when(setOps.members(USER_KEYPREFIX + USER_ID)).thenReturn(Set.of(h1, h2));

        tokenService.revokeAllUserSessions(USER_ID);

        verify(stringRedisTemplate).delete(SESSION + h1);
        verify(stringRedisTemplate).delete(SESSION + h2);
        verify(valueOps).set(eq(USED + h1), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq(USED + h2), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).delete(USER_KEYPREFIX + USER_ID);
        verify(valueOps)
                .set(
                        eq(LoginCacheConstants.FORCE_LOGOUT_KEY + USER_ID), anyString(),
                        anyLong(), eq(TimeUnit.MINUTES));
    }

    // ==================== helpers ====================

    private static Jwt mockJwt(String tokenValue) {
        var jwt = org.mockito.Mockito.mock(Jwt.class);
        lenient().when(jwt.getTokenValue()).thenReturn(tokenValue);
        return jwt;
    }

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
