package com.cartethyia.easyorange.framework.service.impl;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.JwtUtil;
import io.jsonwebtoken.Claims;
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
    public String createAccessToken(Long userId, String username, String userType) {
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
    public void delToken(String token) {
        try {
            jwtUtil.getClaim(token, "uuid", String.class).ifPresent(uuid -> stringRedisTemplate.delete(getTokenKey(uuid)));
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
                    .map(Claims::getSubject)
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
        return createAccessToken(userId, username, userType);
    }

    private String getTokenKey(String uuid) {
        return LoginCacheConstants.buildTokenKey(uuid);
    }
}
