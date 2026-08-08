package com.cartethyia.easyorange.payment.adapter.outbound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "payment.callback")
public class PaymentCallbackProperties {

    /** 回调签名 HMAC 密钥。 */
    private String secret = "default-callback-secret-key";

    /** 是否启用回调签名校验（测试环境可关）。 */
    private boolean verifyEnabled = true;
}
