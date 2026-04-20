package com.cartethyia.easyorange.common.annotation;

import com.cartethyia.easyorange.common.enums.BusinessType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author cartethyia
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Log {
    
    /**
     * 模块名称
     */
    String title() default "";
    
    /**
     * 业务类型
     */
    BusinessType type() default BusinessType.OTHER;
    
    /**
     * 是否保存请求参数
     */
    boolean isSaveRequestData() default true;
    
    /**
     * 是否保存响应参数
     */
    boolean isSaveResponseData() default true;
    
    /**
     * 排除指定的参数名
     */
    String[] excludeParamNames() default {};
}
