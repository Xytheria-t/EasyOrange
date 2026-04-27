package com.cartethyia.easyorange.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重提交注解
 *
 * @author cartethyia
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    
    /**
     * 间隔时间（毫秒）
     */
    long interval() default 3000;
    
    /**
     * 提示消息
     */
    String message() default "不允许重复提交";
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
