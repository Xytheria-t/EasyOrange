package com.cartethyia.easyorange.framework.config.database;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 运行时插件配置（分页、乐观锁）。
 * <p>前缀 {@code mybatis-plus}，示例：</p>
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
@Setter
@AutoConfiguration
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusConfig {

    private PaginationConfig pagination = new PaginationConfig();

    private OptimisticLockConfig optimisticLock = new OptimisticLockConfig();

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();

        if (optimisticLock.enabled) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        if (pagination.enabled) {
            var pageInterceptor = new PaginationInnerInterceptor(DbType.getDbType(pagination.dbType));
            pageInterceptor.setMaxLimit(pagination.maxLimit);
            pageInterceptor.setOverflow(pagination.overflow);
            pageInterceptor.setOptimizeJoin(pagination.optimizeJoin);
            interceptor.addInnerInterceptor(pageInterceptor);
        }

        return interceptor;
    }

    @Setter
    public static class PaginationConfig {
        private boolean enabled = true;
        private String dbType = "mysql";
        private long maxLimit = 100L;
        private boolean overflow = false;
        private boolean optimizeJoin = true;
    }

    @Setter
    public static class OptimisticLockConfig {
        private boolean enabled = true;
    }
}
