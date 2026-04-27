package com.cartethyia.easyorange.framework.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存工具类
 *
 * @deprecated 统一使用 {@link com.cartethyia.easyorange.framework.redis.RedisCache}，
 *             该类支持 key prefix 前缀，便于多环境/多实例隔离。
 *
 * @author cartethyia
 * @date 2026/04/23
 */
@Slf4j
@Component
@Deprecated
@RequiredArgsConstructor
public class CacheUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("设置缓存：key={}", key);
        } catch (Exception e) {
            log.error("设置缓存失败：key={}, error={}", key, e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存失败：key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除缓存失败：key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public void deleteBatch(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(keys);
            log.debug("批量删除缓存：keys={}", keys);
        } catch (Exception e) {
            log.error("批量删除缓存失败：error={}", e.getMessage());
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查缓存失败：key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败：key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取缓存过期时间失败：key={}, error={}", key, e.getMessage());
            return -2L;
        }
    }

    public void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("按前缀删除缓存：prefix={}, count={}", prefix, keys.size());
            }
        } catch (Exception e) {
            log.error("按前缀删除缓存失败：prefix={}, error={}", prefix, e.getMessage());
        }
    }
}