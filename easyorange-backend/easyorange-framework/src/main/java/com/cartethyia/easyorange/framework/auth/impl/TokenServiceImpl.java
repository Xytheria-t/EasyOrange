package com.cartethyia.easyorange.framework.auth.impl;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.LoginCacheConstants;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * 认证令牌实现 — access（RSA JWT）签发/吊销 + refresh（opaque）Redis 存储的统一门面。
 * <p>
 * refresh token 数据模型（前缀 {@link LoginCacheConstants#REFRESH_SESSION_KEY}）：
 * <ul>
 *   <li>SESSION:{tokenHash} → userId（活跃会话，TTL=refresh 生命周期）</li>
 *   <li>USED:{tokenHash} → userId:rotationTs（已消费标记，短 TTL）</li>
 *   <li>USER:{userId} → SET of tokenHash（按用户吊销索引）</li>
 * </ul>
 * token 存 SHA-256 哈希而非原文，Redis 泄露不直接暴露可用明文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";

    /** 已消费标记的保留时长（秒）。过期后复用只按"无效"处理，token 依然不可用。 */
    private static final long USED_MARKER_TTL_SECONDS = 600;

    /** 复用宽限期（毫秒）：期内视为多标签页并发，不吊销；期外视为盗用。 */
    private static final long REUSE_GRACE_MS = 30_000;

    private static final String SESSION_KEY = LoginCacheConstants.REFRESH_SESSION_KEY;
    private static final String USED_KEY = LoginCacheConstants.REFRESH_USED_KEY;
    private static final String USER_KEY = LoginCacheConstants.REFRESH_USER_KEY;

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    // ==================== access token（RSA JWT） ====================

    @Override
    public String createAccessToken(String userId, String username, Collection<String> authorities) {
        var jti = UUID.randomUUID().toString().replace("-", "");
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(jwtProperties.getAccessTokenExpiration(), ChronoUnit.MINUTES))
                .claim("jti", jti)
                .claim("type", ACCESS_TOKEN_TYPE)
                .claim("username", username != null ? username : "")
                .claim("authorities", authorities != null ? List.copyOf(authorities) : List.of())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public void revokeAccessToken(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            String jti = jwt.getId();
            if (jti != null) {
                Instant expiresAt = jwt.getExpiresAt();
                if (expiresAt != null) {
                    var ttl = Duration.between(Instant.now(), expiresAt).getSeconds();
                    if (ttl > 0) {
                        stringRedisTemplate.opsForValue().set(getBlacklistKey(jti), "1", ttl, TimeUnit.SECONDS);
                    }
                }
            }
        } catch (JwtException e) {
            log.warn("吊销 access token - token 已无效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("吊销 access token 失败", e);
        }
    }

    // ==================== refresh token（opaque，Redis） ====================

    @Override
    public String createRefreshToken(String userId) {
        var token = generateToken();
        var hash = sha256(token);
        stringRedisTemplate.opsForValue().set(SESSION_KEY + hash, userId, ttlSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().add(USER_KEY + userId, hash);
        return token;
    }

    @Override
    public TokenRotation rotateRefreshToken(String refreshToken) {
        var hash = sha256(refreshToken);
        var userId = stringRedisTemplate.opsForValue().get(SESSION_KEY + hash);

        if (userId == null) {
            var used = stringRedisTemplate.opsForValue().get(USED_KEY + hash);
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
        stringRedisTemplate.delete(SESSION_KEY + hash);
        stringRedisTemplate.opsForValue()
                .set(
                        USED_KEY + hash,
                        userId + ":" + System.currentTimeMillis(),
                        USED_MARKER_TTL_SECONDS,
                        TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().remove(USER_KEY + userId, hash);

        // 签发新 token（同用户新会话）
        var newToken = generateToken();
        var newHash = sha256(newToken);
        stringRedisTemplate.opsForValue().set(SESSION_KEY + newHash, userId, ttlSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().add(USER_KEY + userId, newHash);
        return new TokenRotation(userId, newToken);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        var hash = sha256(refreshToken);
        var userId = stringRedisTemplate.opsForValue().get(SESSION_KEY + hash);
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(SESSION_KEY + hash);
        stringRedisTemplate.opsForValue()
                .set(
                        USED_KEY + hash,
                        userId + ":" + System.currentTimeMillis(),
                        USED_MARKER_TTL_SECONDS,
                        TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().remove(USER_KEY + userId, hash);
    }

    @Override
    public void revokeAllUserSessions(String userId) {
        revokeAllSessions(userId);
        stringRedisTemplate
                .opsForValue()
                .set(
                        getForceLogoutKey(userId),
                        String.valueOf(System.currentTimeMillis()),
                        jwtProperties.getAccessTokenExpiration(),
                        TimeUnit.MINUTES);
    }

    /** 吊销指定用户全部活跃会话（强制下线 / 改密 / 盗用）。 */
    private void revokeAllSessions(String userId) {
        var hashes = stringRedisTemplate.opsForSet().members(USER_KEY + userId);
        if (hashes != null) {
            for (var h : hashes) {
                stringRedisTemplate.delete(SESSION_KEY + h);
                stringRedisTemplate.opsForValue()
                        .set(
                                USED_KEY + h,
                                userId + ":" + System.currentTimeMillis(),
                                USED_MARKER_TTL_SECONDS,
                                TimeUnit.SECONDS);
            }
            stringRedisTemplate.delete(USER_KEY + userId);
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

    private String getBlacklistKey(String jti) {
        return LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti;
    }

    private String getForceLogoutKey(String userId) {
        return LoginCacheConstants.FORCE_LOGOUT_KEY + userId;
    }
}
