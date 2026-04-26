package com.cartethyia.easyorange.user.cache;

import com.cartethyia.easyorange.user.dto.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务
 * 
 * @author cartethyia
 * @date 2026/04/23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final long USER_CACHE_EXPIRE_HOURS = 12;

    /**
     * 获取用户缓存
     *
     * @param userId 用户 ID
     * @return 缓存的用户信息，不存在返回 null
     */
    public UserVO getUserCache(Long userId) {
        if (userId == null) {
            return null;
        }
        
        String key = buildUserKey(userId);
        try {
            Object cacheValue = redisTemplate.opsForValue().get(key);
            if (cacheValue instanceof UserVO userVO) {
                log.debug("命中用户缓存：userId={}", userId);
                return userVO;
            }
        } catch (Exception e) {
            log.error("获取用户缓存失败：userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 设置用户缓存
     *
     * @param userId 用户 ID
     * @param userVO 用户信息
     */
    public void setUserCache(Long userId, UserVO userVO) {
        if (userId == null || userVO == null) {
            return;
        }
        
        String key = buildUserKey(userId);
        try {
            redisTemplate.opsForValue().set(key, userVO, USER_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("设置用户缓存：userId={}", userId);
        } catch (Exception e) {
            log.error("设置用户缓存失败：userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 删除用户缓存
     *
     * @param userId 用户 ID
     */
    public void deleteUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        String key = buildUserKey(userId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("删除用户缓存：userId={}", userId);
            }
        } catch (Exception e) {
            log.error("删除用户缓存失败：userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 检查用户缓存是否存在
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    public Boolean hasUserCache(Long userId) {
        if (userId == null) {
            return false;
        }
        
        String key = buildUserKey(userId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查用户缓存失败：userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    private String buildUserKey(Long userId) {
        return USER_CACHE_KEY_PREFIX + userId;
    }
}
