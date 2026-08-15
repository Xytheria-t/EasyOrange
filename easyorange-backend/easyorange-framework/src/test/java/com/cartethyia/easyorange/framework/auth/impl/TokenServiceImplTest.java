package com.cartethyia.easyorange.framework.auth.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.auth.RefreshTokenStore;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * TokenService 门面 — 单元测试。
 * <p>
 * 验证：access JWT 签发（type=access）、refresh 委托 opaque 存储、access 黑名单吊销、按用户吊销。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 门面")
class TokenServiceImplTest {

    private static final String USER_ID = "u1";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(jwtProperties.getIssuer()).thenReturn("easyorange");
        lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(30L);
        tokenService =
                new TokenServiceImpl(stringRedisTemplate, jwtEncoder, jwtDecoder, jwtProperties, refreshTokenStore);
    }

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
    @DisplayName("createRefreshToken 委托 opaque 存储")
    void createRefreshToken_delegatesToStore() {
        when(refreshTokenStore.create(USER_ID)).thenReturn("opaque-rt");

        assertThat(tokenService.createRefreshToken(USER_ID)).isEqualTo("opaque-rt");
        verify(refreshTokenStore).create(USER_ID);
    }

    @Test
    @DisplayName("rotateRefreshToken 委托 opaque 存储并返回轮换结果")
    void rotateRefreshToken_delegatesToStore() {
        var expected = new TokenRotation(USER_ID, "new-rt");
        when(refreshTokenStore.rotate("old-rt")).thenReturn(expected);

        assertThat(tokenService.rotateRefreshToken("old-rt")).isEqualTo(expected);
    }

    @Test
    @DisplayName("revokeRefreshToken 委托 opaque 存储")
    void revokeRefreshToken_delegatesToStore() {
        tokenService.revokeRefreshToken("rt");

        verify(refreshTokenStore).revoke("rt");
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

    @Test
    @DisplayName("revokeAllUserSessions 吊销 refresh 会话并设置 access 强制下线标记")
    void revokeAllUserSessions_revokesRefreshAndFlagsForceLogout() {
        tokenService.revokeAllUserSessions(USER_ID);

        verify(refreshTokenStore).revokeAllSessions(USER_ID);
        verify(valueOps)
                .set(
                        eq(LoginCacheConstants.FORCE_LOGOUT_KEY + USER_ID), anyString(),
                        anyLong(), eq(TimeUnit.MINUTES));
    }

    private static Jwt mockJwt(String tokenValue) {
        var jwt = org.mockito.Mockito.mock(Jwt.class);
        lenient().when(jwt.getTokenValue()).thenReturn(tokenValue);
        return jwt;
    }
}
