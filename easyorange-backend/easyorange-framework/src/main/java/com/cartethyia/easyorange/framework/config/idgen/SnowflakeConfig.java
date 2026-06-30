package com.cartethyia.easyorange.framework.config.idgen;

import com.cartethyia.easyorange.framework.config.properties.IdGenProperties;
import com.cartethyia.easyorange.framework.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.idgen.RedisWorkerIdProvider;
import com.cartethyia.easyorange.framework.idgen.SnowflakeIdGenerator;
import com.cartethyia.easyorange.framework.idgen.UuidV7IdGenerator;
import com.cartethyia.easyorange.framework.idgen.WorkerIdProvider;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ID 生成器配置
 * <p>
 * 默认 UUID v7（RFC 9562），零协调零依赖。
 * Snowflake 通过 {@code easyorange.idgen.type=snowflake} 启用。
 */
@Configuration(proxyBeanMethods = false)
public class SnowflakeConfig {

    private final IdGenProperties idGenProperties;

    public SnowflakeConfig(IdGenProperties idGenProperties) {
        this.idGenProperties = idGenProperties;
    }

    /**
     * 默认：UUID v7（RFC 9562）
     */
    @Primary
    @Bean
    public IdGenerator idGenerator() {
        return new UuidV7IdGenerator();
    }

    // ========== Snowflake 备选 ==========

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdProvider workerIdProvider(RedisCache redisCache) {
        return new RedisWorkerIdProvider(redisCache);
    }

    @Bean
    @ConditionalOnProperty(prefix = "easyorange.idgen", name = "type", havingValue = "snowflake")
    public SnowflakeIdGenerator snowflakeIdGenerator(WorkerIdProvider workerIdProvider) {
        return new SnowflakeIdGenerator(workerIdProvider, idGenProperties.getDataCenterId());
    }
}
