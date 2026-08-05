package com.cartethyia.easyorange.framework.auth.impl;

import com.cartethyia.easyorange.framework.auth.RefreshTokenStore;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

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
    public String createRefreshToken(String userId) {
        return refreshTokenStore.create(userId);
    }

    @Override
    public TokenRotation rotateRefreshToken(String refreshToken) {
        return refreshTokenStore.rotate(refreshToken);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
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
                        stringRedisTemplate.opsForValue().set(
                                getBlacklistKey(jti), "1", ttl, TimeUnit.SECONDS
                        );
                    }
                }
            }
        } catch (JwtException e) {
            log.warn("吊销 access token - token 已无效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("吊销 access token 失败", e);
        }
    }

    @Override
    public void revokeAllUserSessions(String userId) {
        refreshTokenStore.revokeAllSessions(userId);
        stringRedisTemplate.opsForValue().set(
                getForceLogoutKey(userId), String.valueOf(System.currentTimeMillis()),
                jwtProperties.getAccessTokenExpiration(), TimeUnit.MINUTES
        );
    }

    private String getBlacklistKey(String jti) {
        return LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti;
    }

    private String getForceLogoutKey(String userId) {
        return LoginCacheConstants.FORCE_LOGOUT_KEY + userId;
    }
}