package com.cartethyia.easyorange.framework.auth.impl;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Override
    public String createAccessToken(String userId, String username, String userType) {
        var jti = UUID.randomUUID().toString().replace("-", "");
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(jwtProperties.getAccessTokenExpiration(), ChronoUnit.MINUTES))
                .claim("jti", jti)
                .claim("type", ACCESS_TOKEN_TYPE)
                .claim("username", username != null ? username : "")
                .claim("userType", userType != null ? userType : "")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String createRefreshToken(String userId, String username, String userType) {
        var jti = UUID.randomUUID().toString().replace("-", "");
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenExpiration(), ChronoUnit.DAYS))
                .claim("jti", jti)
                .claim("type", REFRESH_TOKEN_TYPE)
                .claim("username", username != null ? username : "")
                .claim("userType", userType != null ? userType : "")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public void invalidateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
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
            log.warn("Token 失效 - token 已无效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Token 失效失败", e);
        }
    }

    @Override
    public void invalidateAllUserTokens(String userId) {
        stringRedisTemplate.opsForValue().set(
                getForceLogoutKey(userId), String.valueOf(System.currentTimeMillis()),
                jwtProperties.getAccessTokenExpiration(), TimeUnit.MINUTES
        );
    }

    @Override
    public TokenRefreshResult refreshToken(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        if (!REFRESH_TOKEN_TYPE.equals(jwt.getClaimAsString("type"))) {
            log.warn("尝试使用 access token 刷新，拒绝");
            throw BusinessException.of(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        String userId = jwt.getSubject();

        invalidateToken(refreshToken);

        String username = jwt.getClaimAsString("username");
        String userType = jwt.getClaimAsString("userType");

        String newAccessToken = createAccessToken(userId, username, userType);
        String newRefreshToken = createRefreshToken(userId, username, userType);

        return new TokenRefreshResult(newAccessToken, newRefreshToken);
    }

    private String getBlacklistKey(String jti) {
        return LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti;
    }

    private String getForceLogoutKey(String userId) {
        return LoginCacheConstants.FORCE_LOGOUT_KEY + userId;
    }
}
