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
 *
 * @param pagination 分页插件配置
 * @param optimisticLock 乐观锁插件配置
 */
@Validated
@ConfigurationProperties(prefix = "mybatis-plus")
public record MybatisPlusInterceptorProperties(
        @Valid Pagination pagination, @Valid OptimisticLock optimisticLock) {

    public MybatisPlusInterceptorProperties {
        if (pagination == null) {
            pagination = new Pagination(
                    Pagination.DEFAULT_ENABLED,
                    Pagination.DEFAULT_DB_TYPE,
                    Pagination.DEFAULT_MAX_LIMIT,
                    Pagination.DEFAULT_OVERFLOW,
                    Pagination.DEFAULT_OPTIMIZE_JOIN);
        }
        if (optimisticLock == null) {
            optimisticLock = new OptimisticLock(OptimisticLock.DEFAULT_ENABLED);
        }
    }

    /**
     * 分页插件配置。
     *
     * @param enabled 是否启用分页插件
     * @param dbType 数据库类型（MyBatis-Plus {@code DbType} 名）
     * @param maxLimit 单页条数上限，超出即按该值截断
     * @param overflow 请求页码超出总页数时是否回到首页
     * @param optimizeJoin 是否优化带 JOIN 的分页 count 语句
     */
    public record Pagination(
            @DefaultValue(DEFAULT_ENABLED + "") boolean enabled,
            @NotBlank @DefaultValue(DEFAULT_DB_TYPE) String dbType,
            @Min(1) @DefaultValue(DEFAULT_MAX_LIMIT + "") long maxLimit,
            @DefaultValue(DEFAULT_OVERFLOW + "") boolean overflow,
            @DefaultValue(DEFAULT_OPTIMIZE_JOIN + "") boolean optimizeJoin) {

        private static final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_DB_TYPE = "mysql";
        private static final long DEFAULT_MAX_LIMIT = 100L;
        private static final boolean DEFAULT_OVERFLOW = false;
        private static final boolean DEFAULT_OPTIMIZE_JOIN = true;
    }

    /**
     * 乐观锁插件配置。
     *
     * @param enabled 是否启用（实体需带 {@code @Version} 字段）
     */
    public record OptimisticLock(
            @DefaultValue(DEFAULT_ENABLED + "") boolean enabled) {

        private static final boolean DEFAULT_ENABLED = true;
    }
}
