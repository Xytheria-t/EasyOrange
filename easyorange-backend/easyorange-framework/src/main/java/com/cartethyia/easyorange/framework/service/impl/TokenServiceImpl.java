package com.cartethyia.easyorange.framework.service.impl;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public String createAccessToken(Long userId, String username, String userType) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> claims = buildClaims(jti, username, userType, ACCESS_TOKEN_TYPE);
        return jwtUtil.generateToken(userId.toString(), claims);
    }

    @Override
    public String createRefreshToken(Long userId, String username, String userType) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> claims = buildClaims(jti, username, userType, REFRESH_TOKEN_TYPE);
        return jwtUtil.generateRefreshToken(userId.toString(), claims);
    }

    @Override
    public void invalidateToken(String token) {
        try {
            jwtUtil.parseToken(token).ifPresent(claims -> {
                String jti = claims.get("jti", String.class);
                if (jti != null) {
                    long ttlSeconds = Duration.between(Instant.now(), claims.getExpiration().toInstant()).getSeconds();
                    if (ttlSeconds > 0) {
                        stringRedisTemplate.opsForValue().set(
                            getBlacklistKey(jti), "1", ttlSeconds, TimeUnit.SECONDS
                        );
                    }
                }
            });
        } catch (Exception e) {
            log.error("Token 失效失败：{}", e.getMessage());
        }
    }

    @Override
    public void invalidateAllUserTokens(Long userId) {
        String key = getForceLogoutKey(userId);
        stringRedisTemplate.opsForValue().set(
            key, String.valueOf(System.currentTimeMillis()),
            jwtProperties.getAccessTokenExpiration(), TimeUnit.MINUTES
        );
    }

    @Override
    public Long verifyTokenAndGetUserId(String token) {
        try {
            return jwtUtil.parseToken(token)
                    .filter(claims -> {
                        String jti = claims.get("jti", String.class);
                        return jti != null && !isTokenRevoked(jti);
                    })
                    .filter(claims -> {
                        String forceLogoutTime = stringRedisTemplate.opsForValue()
                            .get(getForceLogoutKey(Long.parseLong(claims.getSubject())));
                        if (forceLogoutTime == null) return true;
                        Date iat = claims.getIssuedAt();
                        return iat != null && iat.getTime() >= Long.parseLong(forceLogoutTime);
                    })
                    .map(Claims::getSubject)
                    .map(Long::parseLong)
                    .orElse(null);
        } catch (Exception e) {
            log.error("验证 Token 失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public TokenRefreshResult refreshToken(String refreshToken) {
        Long userId = verifyTokenAndGetUserId(refreshToken);
        if (userId == null) {
            return null;
        }

        // 验证是 refresh token 类型
        String tokenType = jwtUtil.getClaim(refreshToken, "type", String.class).orElse(null);
        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            log.warn("尝试使用 access token 刷新，拒绝");
            return null;
        }

        // 轮换：作废旧 refresh token，颁发新对
        invalidateToken(refreshToken);

        String username = jwtUtil.getClaim(refreshToken, "username", String.class).orElse(null);
        String userType = jwtUtil.getClaim(refreshToken, "userType", String.class).orElse(null);

        String newAccessToken = createAccessToken(userId, username, userType);
        String newRefreshToken = createRefreshToken(userId, username, userType);

        return new TokenRefreshResult(newAccessToken, newRefreshToken);
    }

    private boolean isTokenRevoked(String jti) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getBlacklistKey(jti)));
    }

    private String getBlacklistKey(String jti) {
        return LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti;
    }

    private String getForceLogoutKey(Long userId) {
        return LoginCacheConstants.FORCE_LOGOUT_KEY + userId;
    }

    private Map<String, Object> buildClaims(String jti, String username, String userType, String tokenType) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("jti", jti);
        claims.put("type", tokenType);
        if (username != null) {
            claims.put("username", username);
        }
        if (userType != null) {
            claims.put("userType", userType);
        }
        return claims;
    }
}
