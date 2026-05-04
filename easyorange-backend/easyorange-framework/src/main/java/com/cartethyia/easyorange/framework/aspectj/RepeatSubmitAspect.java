package com.cartethyia.easyorange.framework.aspectj;

import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;

    @Around("@annotation(repeatSubmit)")
    public Object doAround(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        long intervalMs = repeatSubmit.timeUnit().toMillis(repeatSubmit.interval());
        if (intervalMs <= 0) {
            throw BusinessException.of("防重提交间隔配置错误");
        }

        String key = buildKey(point);

        if (Boolean.FALSE.equals(redisCache.setIfAbsent(key, "1", intervalMs, TimeUnit.MILLISECONDS))) {
            throw BusinessException.of(repeatSubmit.message());
        }

        try {
            return point.proceed();
        } catch (Throwable ex) {
            redisCache.delete(key);
            throw ex;
        }
    }

    private String buildKey(ProceedingJoinPoint point) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw BusinessException.of("无法获取请求信息");
        }

        HttpServletRequest request = attributes.getRequest();
        String userIdentifier = SecurityContextUtil.getCurrentUserId()
            .map(Object::toString)
            .orElseGet(() -> RequestUtil.getClientIp(request));

        String paramsHash = md5(buildParamsString(request, point));

        return CommonConstant.repeatSubmitKey(userIdentifier, request.getRequestURI(), paramsHash);
    }

    private String buildParamsString(HttpServletRequest request, ProceedingJoinPoint point) {
        StringBuilder sb = new StringBuilder();
        request.getParameterMap().forEach((key, values) -> {
            sb.append(key).append('=');
            for (String value : values) {
                sb.append(value).append(',');
            }
            sb.setLength(sb.length() - 1);
            sb.append(';');
        });
        
        Object[] args = point.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg != null && !isFrameworkType(arg.getClass())) {
                    try {
                        sb.append(objectMapper.writeValueAsString(arg));
                    } catch (Exception e) {
                        sb.append(arg.getClass().getSimpleName());
                    }
                }
            }
        }
        
        return sb.toString();
    }

    private boolean isFrameworkType(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith("jakarta.") ||
               className.startsWith("java.") ||
               className.startsWith("org.springframework.");
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
