package com.cartethyia.easyorange.payment.adapter.outbound.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment.callback")
public record PaymentCallbackProperties(
        /** 回调签名 HMAC 密钥。 */
        @NotBlank @DefaultValue("default-callback-secret-key")
        String secret,

        /** 是否启用回调签名校验（测试环境可关）。 */
        @DefaultValue("true") boolean verifyEnabled) {}
