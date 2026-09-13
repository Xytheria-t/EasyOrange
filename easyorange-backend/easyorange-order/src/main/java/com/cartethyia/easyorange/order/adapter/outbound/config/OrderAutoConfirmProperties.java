package com.cartethyia.easyorange.order.adapter.outbound.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 自动确认收货任务配置 — 独立于 {@code order.timeout}，开关/延迟天数/调度 cron 分开管控。
 */
@Validated
@ConfigurationProperties(prefix = "order.auto-confirm")
public record OrderAutoConfirmProperties(
        @DefaultValue("true") boolean enabled,

        /** 发货后多少天自动确认收货 */
        @Min(1) @DefaultValue("7") int autoConfirmDays,

        @NotBlank @DefaultValue("0 0 2 * * ?") String cron) {}
