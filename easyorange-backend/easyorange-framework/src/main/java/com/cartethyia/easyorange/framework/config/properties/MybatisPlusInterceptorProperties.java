package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * MyBatis-Plus 运行时插件配置，前缀 {@code easyorange.mybatis-plus}（与 MyBatis-Plus 自身的
 * {@code mybatis-plus} 前缀区分，避免两边字段互相污染）。示例：
 * <pre>{@code
 * easyorange:
 *   mybatis-plus:
 *     max-limit: 100
 * }</pre>
 *
 * @param maxLimit 单页条数上限，请求超出即按该值截断（与 API 层 {@code PageRequest.MAX_PAGE_SIZE} 口径一致）
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.mybatis-plus")
public record MybatisPlusInterceptorProperties(
        @Min(1) @DefaultValue("100") long maxLimit) {}
