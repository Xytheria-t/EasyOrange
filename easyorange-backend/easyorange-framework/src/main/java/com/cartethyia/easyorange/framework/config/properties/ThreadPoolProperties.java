package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    private int corePoolSize = 10;

    private int maxPoolSize = 20;

    private int queueCapacity = 100;

    private int keepAliveSeconds = 60;

    private int awaitTerminationSeconds = 60;

    private String threadNamePrefix = "async-";
}
