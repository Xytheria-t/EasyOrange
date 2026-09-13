package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easyorange.validation.password")
public record UserValidationProperties(
        /** 弱密码黑名单（校验时直接拒绝）。 */
        Set<String> weakList) {

    public UserValidationProperties {
        weakList = weakList == null ? Set.of() : Set.copyOf(weakList);
    }
}
