package com.cartethyia.easyorange.framework.aspectj;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private static final String RATE_LIMITER_LUA = """
            local key = KEYS[1]
            local count = tonumber(ARGV[1])
            local time = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current > count then
                return current
            end
            if current == 1 then
                redis.call('EXPIRE', key, time)
            end
            return current""";

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(RATE_LIMITER_LUA);
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final RedisCache redisCache;

    public RateLimiterAspect(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        int count = rateLimiter.count();
        long timeInSeconds = rateLimiter.timeUnit().toSeconds(rateLimiter.time());

        if (timeInSeconds <= 0 || count <= 0) {
            throw BusinessException.of("限流参数配置错误");
        }

        String combineKey = getCombineKey(rateLimiter, point);

        List<String> keys = Collections.singletonList(combineKey);
        Long number = redisCache.executeLuaScript(RATE_LIMIT_SCRIPT, keys, count, timeInSeconds);

        if (number == null || number > count) {
            log.warn("action=rate_limit, key={}, current={}, limit={}", combineKey, number, count);
            throw BusinessException.of("请求过于频繁，请稍后再试");
        }

        log.trace("action=rate_limit_check, key={}, current={}, limit={}", combineKey, number, count);
    }

    private String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        String limitType;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            limitType = "unknown";
        } else {
            HttpServletRequest request = attributes.getRequest();
            limitType = switch (rateLimiter.limitType()) {
                case GLOBAL -> "global";
                case IP -> {
                    String clientIp = RequestUtil.getClientIp(request);
                    yield ("unknown".equalsIgnoreCase(clientIp)) ? "unknown" : clientIp;
                }
                case USER -> SecurityContextUtil.getCurrentUserId().map(String::valueOf).orElse("anonymous");
            };
        }
        return CacheConstants.RateLimit.key(limitType, rateLimiter.key());
    }
}