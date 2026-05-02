package com.cartethyia.easyorange.framework.aspectj;

import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RepeatSubmitAspect {

    private static final int MD5_HEX_LENGTH = 32;
    private static final ThreadLocal<MessageDigest> MD5_CACHE = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    });

    private final RedisCache redisCache;

    public RepeatSubmitAspect(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Around("@annotation(repeatSubmit)")
    public Object doAround(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        long interval = repeatSubmit.interval();
        if (interval <= 0) {
            throw BusinessException.of("防重提交间隔配置错误");
        }

        interval = repeatSubmit.timeUnit().toMillis(interval);

        String key = getRepeatSubmitKey(repeatSubmit);

        Boolean exists = redisCache.setIfAbsent(
            key,
            "1",
            interval,
            TimeUnit.MILLISECONDS
        );

        if (Boolean.FALSE.equals(exists)) {
            log.warn("action=repeat_submit, key={}", key);
            throw BusinessException.of(repeatSubmit.message());
        }

        log.debug("action=repeat_submit_check, key={}, interval={}ms", key, interval);

        try {
            return point.proceed();
        } catch (Throwable ex) {
            redisCache.delete(key);
            throw ex;
        } finally {
            MD5_CACHE.remove();
        }
    }

    private String getRepeatSubmitKey(RepeatSubmit repeatSubmit) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw BusinessException.of("无法获取请求信息");
        }

        HttpServletRequest request = attributes.getRequest();

        String params = getRequestParams(request);

        String userIdentifier = getUserIdentifier(request);

        String uri = request.getRequestURI();

        String md5 = md5(params);

        return CommonConstant.repeatSubmitKey(userIdentifier, uri, md5);
    }

    private String getUserIdentifier(HttpServletRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserId().orElse(null);
        if (userId != null) {
            return userId.toString();
        }
        return RequestUtil.getClientIp(request);
    }

    private String getRequestParams(HttpServletRequest request) {
        StringBuilder params = new StringBuilder();

        request.getParameterMap().forEach((key, values) -> {
            params.append(key).append("=");
            for (String value : values) {
                params.append(value).append(",");
            }
            params.append(";");
        });

        return params.toString();
    }

    private String md5(String str) {
        MessageDigest md = MD5_CACHE.get();
        md.reset();
        byte[] bytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(MD5_HEX_LENGTH);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}