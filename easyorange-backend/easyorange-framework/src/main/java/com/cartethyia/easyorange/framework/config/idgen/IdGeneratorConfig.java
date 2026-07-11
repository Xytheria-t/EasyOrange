package com.cartethyia.easyorange.framework.config.idgen;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.idgen.UuidV7IdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ID 生成器配置
 * <p>
 * 默认 UUID v7（RFC 9562），零协调零依赖。
 */
@AutoConfiguration
public class IdGeneratorConfig {

    @Primary
    @Bean
    public IdGenerator idGenerator() {
        return new UuidV7IdGenerator();
    }
}
