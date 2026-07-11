package com.cartethyia.easyorange.order.adapter.outbound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "order.timeout")
public class OrderTimeoutProperties {

    private boolean enabled = true;

    private int timeoutMinutes = 30;

    private String cron = "0 */5 * * * ?";
}