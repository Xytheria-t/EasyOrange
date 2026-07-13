package com.cartethyia.easyorange.framework.web.idempotency;

import com.cartethyia.easyorange.common.annotation.Idempotent;
import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Idempotency-Key 幂等切面。
 * <p>
 * 拦截所有标注 {@code @Idempotent} 的 Controller 方法，
 * 提取客户端提供的幂等 key，通过 {@link IdempotencyService} 确保重复请求幂等。
 * </p>
 * <p>
 * 执行顺序在 {@link com.cartethyia.easyorange.framework.audit.aspect.AuditLogAspect @Order(3)} 之前，
 * 确保幂等拦截在审计日志前，避免重复执行记录两次日志。
 * </p>
 */
@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;

    @Around("@annotation(idempotent)")
    public Object aroundIdempotentMethod(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        HttpServletRequest request = RequestUtil.getRequest();
        if (request == null) {
            return pjp.proceed();
        }

        String key = request.getHeader(idempotent.headerName());
        if (key == null || key.isBlank()) {
            // 未提供幂等 key → 正常执行（不强制客户端使用）
            return pjp.proceed();
        }

        log.debug("action=idempotency_execute, key={}, uri={}", key, request.getRequestURI());
        return idempotencyService.execute(key, idempotent.ttlSeconds(), pjp::proceed);
    }
}
