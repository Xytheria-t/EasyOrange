package com.cartethyia.easyorange.framework.config.database;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 编程式事务模板配置 — 供「锁等待在事务外、业务流程在事务内」的用例（下单、超时取消）精确控制事务边界；
 * Spring Boot 若已自动装配 TransactionTemplate 则本 Bean 自动跳过（双方均带 {@code @ConditionalOnMissingBean}，
 * 无论装配顺序如何都不会重复定义）。
 */
@AutoConfiguration
public class TransactionTemplateConfig {

    @Bean
    @ConditionalOnMissingBean(TransactionTemplate.class)
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
