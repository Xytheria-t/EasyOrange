package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 密码强度校验配置。
 *
 * @param weakList 弱密码黑名单（校验时直接拒绝），缺省收敛为空集合
 */
@ConfigurationProperties(prefix = "easyorange.validation.password")
public record UserValidationProperties(Set<String> weakList) {

    public UserValidationProperties {
        weakList = weakList == null ? Set.of() : Set.copyOf(weakList);
    }
}
