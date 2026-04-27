package com.cartethyia.easyorange.common.annotation;

import com.cartethyia.easyorange.common.enums.LimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 *
 * @author cartethyia
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    
    /**
     * 限流 key（支持 SpEL 表达式，如 "#userId"）
     * <p>
     * 必须显式指定，避免所有接口共享同一限流计数器。
     * </p>
     */
    String key();
    
    /**
     * 限流时间（秒）
     */
    int time() default 60;
    
    /**
     * 限流次数
     */
    int count() default 100;
    
    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.GLOBAL;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流触发时的提示信息
     */
    String message() default "请求过于频繁，请稍后重试";
}
