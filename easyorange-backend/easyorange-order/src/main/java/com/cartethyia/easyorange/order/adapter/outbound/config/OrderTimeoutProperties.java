package com.cartethyia.easyorange.order.adapter.outbound.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 订单超时取消任务配置 — 开关与超时分钟分开管控。
 * <p>
 * 调度 cron 不在这里声明：{@code @Scheduled} 只能从 Environment 取值，两处各写一份默认值必然漂移，
 * 故 cron 单一来源是 {@code OrderTimeoutTask} 上注解里的 {@code ${order.timeout.cron:...}}。
 */
@Validated
@ConfigurationProperties(prefix = "order.timeout")
public record OrderTimeoutProperties(
        @DefaultValue("true") boolean enabled,
        @Min(1) @DefaultValue("30") int timeoutMinutes) {}
