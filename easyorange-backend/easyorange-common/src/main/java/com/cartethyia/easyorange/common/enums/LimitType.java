package com.cartethyia.easyorange.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 限流类型枚举
 * <p>
 * 用于 {@code @RateLimiter} 注解，指定限流的维度。
 * </p>
 *
 * @author cartethyia
 */
@Getter
@AllArgsConstructor
public enum LimitType {

    /**
     * 全局限流（所有请求共享限流配额）
     */
    GLOBAL("全局"),

    /**
     * IP 限流（每个 IP 独立限流）
     */
    IP("IP"),

    /**
     * 用户限流（每个登录用户独立限流）
     */
    USER("用户");

    private final String description;

}
