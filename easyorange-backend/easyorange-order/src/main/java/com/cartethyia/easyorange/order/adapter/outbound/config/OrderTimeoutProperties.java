package com.cartethyia.easyorange.order.adapter.outbound.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "order.timeout")
public record OrderTimeoutProperties(
        @DefaultValue("true") boolean enabled,

        @Min(1) @DefaultValue("30") int timeoutMinutes,

        @NotBlank @DefaultValue("0 */5 * * * ?") String cron) {}
