package com.cartethyia.easyorange.framework.config.idgen;

import com.cartethyia.easyorange.framework.config.properties.IdGenProperties;
import com.cartethyia.easyorange.framework.idgen.RedisWorkerIdProvider;
import com.cartethyia.easyorange.framework.idgen.SnowflakeIdGenerator;
import com.cartethyia.easyorange.framework.idgen.WorkerIdProvider;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "easyorange.idgen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowflakeConfig {

    private final IdGenProperties idGenProperties;

    public SnowflakeConfig(IdGenProperties idGenProperties) {
        this.idGenProperties = idGenProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdProvider workerIdProvider(RedisCache redisCache) {
        return new RedisWorkerIdProvider(redisCache);
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(WorkerIdProvider workerIdProvider) {
        return new SnowflakeIdGenerator(workerIdProvider, idGenProperties.getDataCenterId());
    }
}