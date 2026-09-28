package com.cartethyia.easyorange.order.adapter.outbound.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 自动确认收货任务配置 — 独立于 {@code order.timeout}，开关与延迟天数分开管控。
 * <p>
 * 调度 cron 不在这里声明：{@code @Scheduled} 只能从 Environment 取值，两处各写一份默认值必然漂移，
 * 故 cron 单一来源是注解上的 {@code ${order.auto-confirm.cron:...}}，改调度只改那一行。
 *
 * @param enabled 是否启用自动确认收货任务
 * @param autoConfirmDays 发货后多少天自动确认收货
 */
@Validated
@ConfigurationProperties(prefix = "order.auto-confirm")
public record OrderAutoConfirmProperties(
        @DefaultValue("true") boolean enabled,
        @Min(1) @DefaultValue("7") int autoConfirmDays) {}
