package com.cartethyia.easyorange.framework.auth.impl;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.auth.RefreshTokenStore;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 不透明 refresh token 的 Redis 存储实现。
 * <p>
 * 数据模型（前缀 {@link LoginCacheConstants#REFRESH_SESSION_KEY}）：
 * <ul>
 *   <li>SESSION:{tokenHash} → userId（活跃会话，TTL=refresh 生命周期）</li>
 *   <li>USED:{tokenHash} → userId:rotationTs（已消费标记，短 TTL）</li>
 *   <li>USER:{userId} → SET of tokenHash（按用户吊销索引）</li>
 * </ul>
 * token 存 SHA-256 哈希而非原文，Redis 泄露不直接暴露可用明文。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStoreImpl implements RefreshTokenStore {

    /** 已消费标记的保留时长（秒）。过期后复用只按"无效"处理，token 依然不可用。 */
    private static final long USED_MARKER_TTL_SECONDS = 600;

    /** 复用宽限期（毫秒）：期内视为多标签页并发，不吊销；期外视为盗用。 */
    private static final long REUSE_GRACE_MS = 30_000;

    private static final String SESSION_KEY = LoginCacheConstants.REFRESH_SESSION_KEY;
    private static final String USED_KEY = LoginCacheConstants.REFRESH_USED_KEY;
    private static final String USER_KEY = LoginCacheConstants.REFRESH_USER_KEY;

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;

    @Override
    public String create(String userId) {
        var token = generateToken();
        var hash = sha256(token);
        redis.opsForValue().set(SESSION_KEY + hash, userId, ttlSeconds(), TimeUnit.SECONDS);
        redis.opsForSet().add(USER_KEY + userId, hash);
        return token;
    }

    @Override
    public TokenRotation rotate(String refreshToken) {
        var hash = sha256(refreshToken);
        var userId = redis.opsForValue().get(SESSION_KEY + hash);

        if (userId == null) {
            var used = redis.opsForValue().get(USED_KEY + hash);
            if (used != null) {
                int sep = used.lastIndexOf(':');
                var usedUserId = used.substring(0, sep);
                var usedTs = Long.parseLong(used.substring(sep + 1));
                if (System.currentTimeMillis() - usedTs >= REUSE_GRACE_MS) {
                    revokeAllSessions(usedUserId);
                    throw BusinessException.of(ResultCode.UNAUTHORIZED, "检测到令牌复用，已强制下线，请重新登录");
                }
                throw BusinessException.of(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
            }
            throw BusinessException.of(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        // 消费旧 token
        redis.delete(SESSION_KEY + hash);
        redis.opsForValue()
                .set(
                        USED_KEY + hash,
                        userId + ":" + System.currentTimeMillis(),
                        USED_MARKER_TTL_SECONDS,
                        TimeUnit.SECONDS);
        redis.opsForSet().remove(USER_KEY + userId, hash);

        // 签发新 token（同用户新会话）
        var newToken = generateToken();
        var newHash = sha256(newToken);
        redis.opsForValue().set(SESSION_KEY + newHash, userId, ttlSeconds(), TimeUnit.SECONDS);
        redis.opsForSet().add(USER_KEY + userId, newHash);
        return new TokenRotation(userId, newToken);
    }

    @Override
    public void revoke(String refreshToken) {
        var hash = sha256(refreshToken);
        var userId = redis.opsForValue().get(SESSION_KEY + hash);
        if (userId == null) {
            return;
        }
        redis.delete(SESSION_KEY + hash);
        redis.opsForValue()
                .set(
                        USED_KEY + hash,
                        userId + ":" + System.currentTimeMillis(),
                        USED_MARKER_TTL_SECONDS,
                        TimeUnit.SECONDS);
        redis.opsForSet().remove(USER_KEY + userId, hash);
    }

    @Override
    public void revokeAllSessions(String userId) {
        var hashes = redis.opsForSet().members(USER_KEY + userId);
        if (hashes != null) {
            for (var h : hashes) {
                redis.delete(SESSION_KEY + h);
                redis.opsForValue()
                        .set(
                                USED_KEY + h,
                                userId + ":" + System.currentTimeMillis(),
                                USED_MARKER_TTL_SECONDS,
                                TimeUnit.SECONDS);
            }
            redis.delete(USER_KEY + userId);
        }
    }

    private long ttlSeconds() {
        return Duration.ofDays(jwtProperties.getRefreshTokenExpiration()).getSeconds();
    }

    private static String generateToken() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
