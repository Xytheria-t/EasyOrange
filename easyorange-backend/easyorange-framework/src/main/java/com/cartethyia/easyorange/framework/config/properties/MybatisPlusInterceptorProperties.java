package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
@Data
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusInterceptorProperties {

    private Pagination pagination = new Pagination();

    private OptimisticLock optimisticLock = new OptimisticLock();

    @Data
    public static class Pagination {
        private boolean enabled = true;
        private String dbType = "mysql";
        private long maxLimit = 100L;
        private boolean overflow = false;
        private boolean optimizeJoin = true;
    }

    @Data
    public static class OptimisticLock {
        private boolean enabled = true;
    }
}
