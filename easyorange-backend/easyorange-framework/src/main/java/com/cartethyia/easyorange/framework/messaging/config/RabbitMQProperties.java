package com.cartethyia.easyorange.framework.messaging.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "easyorange.rabbitmq")
public class RabbitMQProperties {

    private boolean enabled = true;

    private RetryConfig retry = new RetryConfig();
    private ConsumerConfig consumer = new ConsumerConfig();

    @Data
    public static class ConsumerConfig {
        private int prefetch = 10;
        private boolean defaultRequeueRejected = false;
        private ConcurrencyConfig concurrency = new ConcurrencyConfig();
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration initialInterval = Duration.ofSeconds(1);
        private double multiplier = 2.0;
        private Duration maxDelay = Duration.ofSeconds(10);
    }

    @Data
    public static class ConcurrencyConfig {
        private int min = 1;
        private int max = 5;
    }
}
