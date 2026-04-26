package com.cartethyia.easyorange.framework.config.database;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * <p>
 * 统一注册 MyBatis-Plus 运行时插件，包括：
 * </p>
 * <ul>
 *     <li>乐观锁插件 - 支持基于版本号的乐观锁控制（需实体类标注 {@code @Version}）</li>
 *     <li>分页插件 - 自动物理分页，支持 MySQL 方言</li>
 * </ul>
 * <p>
 * 配置示例：
 * </p>
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
 * @author cartethyia
 */
@Slf4j
@Getter
@Setter
@ToString
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusConfig {

    /**
     * 分页配置
     */
    private PaginationConfig paginationConfig = new PaginationConfig();

    /**
     * 乐观锁配置
     */
    private OptimisticLockConfig optimisticLockConfig = new OptimisticLockConfig();

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        if (optimisticLockConfig.enabled) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            log.info("MyBatis-Plus 乐观锁插件已启用");
        }

        if (paginationConfig.enabled) {
            interceptor.addInnerInterceptor(paginationInnerInterceptor());
            log.info("MyBatis-Plus 分页插件已启用 - 数据库类型：{}, 最大限制：{}",
                    paginationConfig.dbType, paginationConfig.maxLimit);
        }

        return interceptor;
    }

    private PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor(
                DbType.getDbType(paginationConfig.dbType)
        );
        interceptor.setMaxLimit(paginationConfig.maxLimit);
        interceptor.setOverflow(paginationConfig.overflow);
        interceptor.setOptimizeJoin(paginationConfig.optimizeJoin);
        return interceptor;
    }

    /**
     * 分页配置
     */
    @Getter
    @Setter
    @ToString
    public static class PaginationConfig {
        /**
         * 是否启用分页插件，默认启用
         */
        private boolean enabled = true;

        /**
         * 数据库类型
         * <p>
         * 支持：mysql, postgresql, oracle, sqlserver, h2 等
         * </p>
         */
        private String dbType = "mysql";

        /**
         * 分页最大限制条数
         * <p>
         * 防止恶意查询导致性能问题，默认 100
         * 当请求的 pageSize > maxLimit 时，自动截断为 maxLimit
         * </p>
         */
        private Long maxLimit = 100L;

        /**
         * 是否处理分页溢出
         * <p>
         * true: 当请求页码超过总页数时，返回最后一页数据
         * false: 当请求页码超过总页数时，返回空列表
         * </p>
         */
        private Boolean overflow = false;

        /**
         * 是否优化 JOIN 查询
         * <p>
         * true: 对 JOIN 查询进行优化，提升性能
         * false: 不优化
         * </p>
         */
        private Boolean optimizeJoin = true;
    }

    /**
     * 乐观锁配置
     */
    @Getter
    @Setter
    @ToString
    public static class OptimisticLockConfig {
        /**
         * 是否启用乐观锁插件，默认启用
         * <p>
         * 启用后，实体类的 {@code @Version} 字段将在更新时自动递增
         * </p>
         */
        private boolean enabled = true;
    }
}
