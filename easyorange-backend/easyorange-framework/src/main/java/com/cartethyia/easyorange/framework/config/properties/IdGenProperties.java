package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "easyorange.idgen")
public class IdGenProperties {

    /**
     * ID 生成器类型：uuidv7（默认） | snowflake
     */
    private String type = "uuidv7";

    private long dataCenterId = 1L;
}