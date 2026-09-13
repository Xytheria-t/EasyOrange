package com.cartethyia.easyorange.framework.config.database;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cartethyia.easyorange.framework.config.properties.MybatisPlusInterceptorProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 运行时插件配置（分页、乐观锁），属性见 {@link MybatisPlusInterceptorProperties}。
 */
@AutoConfiguration
@EnableConfigurationProperties(MybatisPlusInterceptorProperties.class)
public class MybatisPlusConfig {

    private final MybatisPlusInterceptorProperties properties;

    public MybatisPlusConfig(MybatisPlusInterceptorProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();

        if (properties.optimisticLock().enabled()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        var pagination = properties.pagination();
        if (pagination.enabled()) {
            var pageInterceptor = new PaginationInnerInterceptor(DbType.getDbType(pagination.dbType()));
            pageInterceptor.setMaxLimit(pagination.maxLimit());
            pageInterceptor.setOverflow(pagination.overflow());
            pageInterceptor.setOptimizeJoin(pagination.optimizeJoin());
            interceptor.addInnerInterceptor(pageInterceptor);
        }

        return interceptor;
    }
}
