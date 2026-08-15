package com.cartethyia.easyorange.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

/**
 * 生产必需密钥契约 — 启动期 fail-fast 校验，替代手写 ApplicationRunner。
 * <p>
 * 与 {@code application-prod.yaml} 的 {@code ${...:}} 无默认值项对齐：环境变量经 relaxed binding
 * 映射到本 record（{@code EASYORANGE_DB_HOST} → {@code easyorange.prod.db-host} → {@code dbHost}），
 * 缺失时占位符解析失败直接中止启动，值为空串时由 {@code @NotBlank} 兜底。
 * 仅 {@code prod} profile 生效（dev 密钥有默认值或自动生成，无需强制）；
 * AI 密钥（DEEPSEEK/QWEN_VL/EMBEDDING）按既定契约可选、降级装配，不在此列。
 */
@Validated
@Profile("prod")
@ConfigurationProperties(prefix = "easyorange.prod")
public record ProdSecrets(
        @NotBlank(message = "EASYORANGE_DB_HOST 缺失") String dbHost,
        @NotBlank(message = "EASYORANGE_DB_USERNAME 缺失") String dbUsername,
        @NotBlank(message = "EASYORANGE_DB_PASSWORD 缺失") String dbPassword,
        @NotBlank(message = "REDIS_HOST 缺失") String redisHost,
        @NotBlank(message = "REDIS_PASSWORD 缺失") String redisPassword,
        @NotBlank(message = "RABBITMQ_HOST 缺失") String rabbitmqHost,
        @NotBlank(message = "RABBITMQ_USER 缺失") String rabbitmqUser,
        @NotBlank(message = "RABBITMQ_PASSWORD 缺失") String rabbitmqPassword,
        @NotBlank(message = "JWT_RSA_PRIVATE_KEY 缺失") String jwtRsaPrivateKey,
        @NotBlank(message = "JWT_RSA_PUBLIC_KEY 缺失") String jwtRsaPublicKey) {}
