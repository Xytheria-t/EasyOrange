package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.validation.password")
public class UserValidationProperties {

    /** 弱密码黑名单（校验时直接拒绝）。 */
    private Set<String> weakList = Set.of();
}
