package com.cartethyia.easyorange.order.adapter.outbound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自动确认收货任务配置 — 独立于 {@code order.timeout}，开关/延迟天数/调度 cron 分开管控。
 */
@Data
@ConfigurationProperties(prefix = "order.auto-confirm")
public class OrderAutoConfirmProperties {

    private boolean enabled = true;

    /** 发货后多少天自动确认收货 */
    private int autoConfirmDays = 7;

    private String cron = "0 0 2 * * ?";
}
