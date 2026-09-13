package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * MyBatis-Plus 拦截器（分页、乐观锁）配置，前缀 {@code mybatis-plus}。示例：
 * <pre>{@code
 * mybatis-plus:
 *   pagination:
 *     enabled: true
 *     db-type: mysql
 *     max-limit: 100
 *     overflow: false
 *     optimize-join: true
 *   optimistic-lock:
 *     enabled: true
 * }</pre>
 */
@Validated
@ConfigurationProperties(prefix = "mybatis-plus")
public record MybatisPlusInterceptorProperties(
        @Valid Pagination pagination, @Valid OptimisticLock optimisticLock) {

    public MybatisPlusInterceptorProperties {
        if (pagination == null) {
            pagination = new Pagination(true, "mysql", 100L, false, true);
        }
        if (optimisticLock == null) {
            optimisticLock = new OptimisticLock(true);
        }
    }

    public record Pagination(
            @DefaultValue("true") boolean enabled,
            @NotBlank @DefaultValue("mysql") String dbType,
            @Min(1) @DefaultValue("100") long maxLimit,
            @DefaultValue("false") boolean overflow,
            @DefaultValue("true") boolean optimizeJoin) {}

    public record OptimisticLock(@DefaultValue("true") boolean enabled) {}
}
