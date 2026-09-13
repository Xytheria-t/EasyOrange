package com.cartethyia.easyorange.framework.config.database;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cartethyia.easyorange.framework.config.properties.MybatisPlusInterceptorProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 运行时插件配置：乐观锁（实体 {@code @Version} 生效）+ 分页（单页上限见
 * {@link MybatisPlusInterceptorProperties}）。
 * <p>
 * 必须显式早于 {@link MybatisPlusInnerInterceptorAutoConfiguration} 装配：后者带
 * {@code @ConditionalOnMissingBean(MybatisPlusInterceptor.class)}，若顺序不定，容器一旦出现别的
 * {@code InnerInterceptor} Bean，它会先注册聚合器，与本类形成两个 {@code MybatisPlusInterceptor}，
 * 分页与乐观锁会在同一段 SQL 上各执行两次；聚合器退让后不再自动收集，新增内置拦截器请加在本类。
 */
@AutoConfiguration(before = MybatisPlusInnerInterceptorAutoConfiguration.class)
@EnableConfigurationProperties(MybatisPlusInterceptorProperties.class)
public class MybatisPlusConfig {

    private final MybatisPlusInterceptorProperties properties;

    public MybatisPlusConfig(MybatisPlusInterceptorProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(properties.maxLimit());

        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页最后添加：作用于前面拦截器改写后的最终 SQL
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
