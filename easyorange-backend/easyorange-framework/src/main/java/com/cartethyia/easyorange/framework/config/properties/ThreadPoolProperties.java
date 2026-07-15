package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    @Min(1) private int corePoolSize = 10;
    @Min(1) private int maxPoolSize = 20;
    @Min(1) private int queueCapacity = 100;
    @Min(1) private int keepAliveSeconds = 60;
    @Min(1) private int awaitTerminationSeconds = 60;
    @NotBlank private String threadNamePrefix = "async-";
}
