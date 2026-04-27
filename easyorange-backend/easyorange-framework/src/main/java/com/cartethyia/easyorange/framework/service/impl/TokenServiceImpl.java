package com.cartethyia.easyorange.framework.service.impl;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public String createToken(Long userId, String username, String userType) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String userKey = getTokenKey(uuid);
        stringRedisTemplate.opsForValue().set(userKey, userId.toString(),
                jwtProperties.getAccessTokenExpiration(), TimeUnit.MINUTES);

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("uuid", uuid);
        if (username != null) {
            claims.put("username", username);
        }
        if (userType != null) {
            claims.put("userType", userType);
        }
        return jwtUtil.generateToken(userId.toString(), claims);
    }

    @Override
    public String createToken(Long userId, String username) {
        return createToken(userId, username, null);
    }

    @Override
    @Deprecated(since = "use verifyTokenAndGetUserId instead")
    public boolean verifyToken(String token) {
        try {
            boolean valid = jwtUtil.validateToken(token);
            if (!valid) {
                log.debug("Token validation failed");
                return false;
            }
            String uuid = jwtUtil.getClaim(token, "uuid", String.class).orElse(null);
            if (uuid == null) {
                log.debug("Token uuid not found");
                return false;
            }
            String userKey = getTokenKey(uuid);
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(userKey))) {
                log.debug("Token not found in Redis, may have been revoked");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("验证 Token 失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long getUserId(String token) {
        try {
            return jwtUtil.getSubject(token)
                .map(Long::parseLong)
                .orElse(null);
        } catch (Exception e) {
            log.error("获取用户 ID 失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public void delToken(String token) {
        try {
            String uuid = jwtUtil.getClaim(token, "uuid", String.class).orElse(null);
            if (uuid != null) {
                stringRedisTemplate.delete(getTokenKey(uuid));
            }
        } catch (Exception e) {
            log.error("删除 Token 失败：{}", e.getMessage());
        }
    }

    @Override
    public Long verifyTokenAndGetUserId(String token) {
        try {
            return jwtUtil.parseToken(token)
                    .filter(claims -> {
                        String uuid = claims.get("uuid", String.class);
                        if (uuid == null) {
                            return false;
                        }
                        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getTokenKey(uuid)));
                    })
                    .map(claims -> claims.getSubject())
                    .map(Long::parseLong)
                    .orElse(null);
        } catch (Exception e) {
            log.error("验证 Token 并获取用户 ID 失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String refreshToken(String token) {
        Long userId = verifyTokenAndGetUserId(token);
        if (userId == null) {
            return null;
        }
        String username = jwtUtil.getClaim(token, "username", String.class).orElse(null);
        String userType = jwtUtil.getClaim(token, "userType", String.class).orElse(null);
        delToken(token);
        return createToken(userId, username, userType);
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.Login.tokenKey(uuid);
    }
}
