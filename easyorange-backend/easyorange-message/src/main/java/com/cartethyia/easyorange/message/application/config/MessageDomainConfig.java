package com.cartethyia.easyorange.message.application.config;

import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息模块领域服务装配 — domain 层零 Spring 依赖（ArchUnit 白名单约束），
 * 由本配置类在 application 边界显式接线。
 */
@Configuration
public class MessageDomainConfig {

    @Bean
    public SensitiveWordFilterService sensitiveWordFilterService() {
        return new SensitiveWordFilterService();
    }
}
