package com.cartethyia.easyorange.framework.service;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.framework.config.JwtProperties;
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
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public String createToken(Long userId, String username) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String userKey = getTokenKey(uuid);
        stringRedisTemplate.opsForValue().set(userKey, userId.toString(),
                jwtProperties.getAccessTokenExpiration(), TimeUnit.MINUTES);

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("uuid", uuid);
        if (username != null) {
            claims.put("username", username);
        }
        return jwtUtil.generateToken(userId.toString(), claims);
    }

    @Deprecated(since = "use verifyTokenAndGetUserId instead")
    public boolean verifyToken(String token) {
        try {
            boolean valid = jwtUtil.validateToken(token);
            if (!valid) {
                log.debug("Token validation failed");
                return false;
            }
            // 从 JWT 中提取 uuid
            String uuid = jwtUtil.getClaim(token, "uuid", String.class).orElse(null);
            if (uuid == null) {
                log.debug("Token uuid not found");
                return false;
            }
            // 检查 Redis 中 token 是否存在
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

    public String refreshToken(String token) {
        Long userId = verifyTokenAndGetUserId(token);
        if (userId == null) {
            return null;
        }
        String username = jwtUtil.getClaim(token, "username", String.class).orElse(null);
        delToken(token);
        return createToken(userId, username);
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.Login.tokenKey(uuid);
    }
}