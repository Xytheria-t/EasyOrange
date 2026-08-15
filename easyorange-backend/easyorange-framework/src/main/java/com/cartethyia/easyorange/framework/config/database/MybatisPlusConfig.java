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

        if (properties.getOptimisticLock().isEnabled()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        var pagination = properties.getPagination();
        if (pagination.isEnabled()) {
            var pageInterceptor = new PaginationInnerInterceptor(DbType.getDbType(pagination.getDbType()));
            pageInterceptor.setMaxLimit(pagination.getMaxLimit());
            pageInterceptor.setOverflow(pagination.isOverflow());
            pageInterceptor.setOptimizeJoin(pagination.isOptimizeJoin());
            interceptor.addInnerInterceptor(pageInterceptor);
        }

        return interceptor;
    }
}
