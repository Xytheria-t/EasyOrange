package com.cartethyia.easyorange.framework.auth.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.auth.RefreshTokenStore;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * RefreshTokenStore Redis 实现 — 单元测试。
 * <p>
 * 验证：create / rotate（含复用检测）/ revoke / revokeAllSessions 的 Redis 命令编排与业务语义。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenStore Redis 实现")
class RefreshTokenStoreImplTest {

    private static final String SESSION = LoginCacheConstants.REFRESH_SESSION_KEY;
    private static final String USED = LoginCacheConstants.REFRESH_USED_KEY;
    private static final String USER_KEYPREFIX = LoginCacheConstants.REFRESH_USER_KEY;
    private static final String USER_ID = "u1";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private JwtProperties jwtProperties;

    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        // setOps / jwtProperties 桩非全部用例使用，标记 lenient
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(7L);
        store = new RefreshTokenStoreImpl(redis, jwtProperties);
    }

    // ==================== create ====================

    @Test
    @DisplayName("create 写入 SESSION 并登记到 USER 集合")
    void create_storesSessionAndRegistersUser() {
        var token = store.create(USER_ID);

        assertThat(token).isNotBlank();
        var hash = sha256(token);
        verify(valueOps).set(eq(SESSION + hash), eq(USER_ID), eq(7L * 24 * 3600), eq(TimeUnit.SECONDS));
        verify(setOps).add(USER_KEYPREFIX + USER_ID, hash);
    }

    @Test
    @DisplayName("create 每次生成不同 token")
    void create_generatesUniqueTokens() {
        var t1 = store.create(USER_ID);
        var t2 = store.create(USER_ID);

        assertThat(t1).isNotEqualTo(t2);
    }

    // ==================== rotate (正常) ====================

    @Test
    @DisplayName("rotate 消费旧 token：删 SESSION、置 USED、发新 token 并迁移 USER 集合")
    void rotate_consumesOldAndIssuesNew() {
        var oldToken = "old-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(USER_ID);

        var rotation = store.rotate(oldToken);

        assertThat(rotation.userId()).isEqualTo(USER_ID);
        assertThat(rotation.newToken()).isNotBlank();
        verify(redis).delete(SESSION + oldHash);
        verify(valueOps).set(eq(USED + oldHash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        var newHash = sha256(rotation.newToken());
        verify(valueOps).set(eq(SESSION + newHash), eq(USER_ID), any(Long.class), eq(TimeUnit.SECONDS));
        verify(setOps).remove(USER_KEYPREFIX + USER_ID, oldHash);
        verify(setOps).add(USER_KEYPREFIX + USER_ID, newHash);
    }

    // ==================== rotate (复用检测) ====================

    @Test
    @DisplayName("rotate 复用超宽限：吊销该用户全部会话并抛 401")
    void rotate_reuseBeyondGrace_revokesAllAndThrows() {
        var oldToken = "used-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(null);
        var staleTs = System.currentTimeMillis() - 60_000;
        when(valueOps.get(USED + oldHash)).thenReturn(USER_ID + ":" + staleTs);
        var otherHash = "other-active-hash";
        when(setOps.members(USER_KEYPREFIX + USER_ID)).thenReturn(Set.of(oldHash, otherHash));

        assertThatThrownBy(() -> store.rotate(oldToken)).isInstanceOf(BusinessException.class);

        // 吊销所有会话
        verify(redis).delete(SESSION + otherHash);
        verify(valueOps).set(eq(USED + otherHash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(redis).delete(USER_KEYPREFIX + USER_ID);
    }

    @Test
    @DisplayName("rotate 复用未超宽限：仅抛 401，不吊销（多标签页并发）")
    void rotate_reuseWithinGrace_throwsWithoutRevoke() {
        var oldToken = "concurrent-token";
        var oldHash = sha256(oldToken);
        when(valueOps.get(SESSION + oldHash)).thenReturn(null);
        var freshTs = System.currentTimeMillis();
        when(valueOps.get(USED + oldHash)).thenReturn(USER_ID + ":" + freshTs);

        assertThatThrownBy(() -> store.rotate(oldToken)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(setOps);
    }

    @Test
    @DisplayName("rotate 未知/过期 token 抛 401")
    void rotate_unknownToken_throws() {
        var token = "expired-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(null);
        when(valueOps.get(USED + hash)).thenReturn(null);

        assertThatThrownBy(() -> store.rotate(token)).isInstanceOf(BusinessException.class);
    }

    // ==================== revoke (登出) ====================

    @Test
    @DisplayName("revoke 删除 SESSION、置 USED、从 USER 集合移除")
    void revoke_removesSessionAndMarksUsed() {
        var token = "revoke-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(USER_ID);

        store.revoke(token);

        verify(redis).delete(SESSION + hash);
        verify(valueOps).set(eq(USED + hash), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(setOps).remove(USER_KEYPREFIX + USER_ID, hash);
    }

    @Test
    @DisplayName("revoke 已失效 token 为幂等，不抛错")
    void revoke_alreadyGone_isNoOp() {
        var token = "gone-token";
        var hash = sha256(token);
        when(valueOps.get(SESSION + hash)).thenReturn(null);

        store.revoke(token);

        verify(redis, never()).delete(SESSION + hash);
    }

    // ==================== revokeAllSessions ====================

    @Test
    @DisplayName("revokeAllSessions 吊销全部会话并删除用户索引")
    void revokeAll_removesAllSessionsAndIndex() {
        var h1 = "h1";
        var h2 = "h2";
        when(setOps.members(USER_KEYPREFIX + USER_ID)).thenReturn(Set.of(h1, h2));

        store.revokeAllSessions(USER_ID);

        verify(redis).delete(SESSION + h1);
        verify(redis).delete(SESSION + h2);
        verify(valueOps).set(eq(USED + h1), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq(USED + h2), anyString(), any(Long.class), eq(TimeUnit.SECONDS));
        verify(redis).delete(USER_KEYPREFIX + USER_ID);
    }

    // ==================== helpers ====================

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
