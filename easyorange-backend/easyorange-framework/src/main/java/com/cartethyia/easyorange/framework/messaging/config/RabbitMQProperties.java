package com.cartethyia.easyorange.framework.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import java.time.Duration;
import java.util.List;

@Data
@Primary
@ConfigurationProperties(prefix = "easyorange.rabbitmq")
public class RabbitMQProperties {

    private boolean enabled = true;
    private String exchange = "eo.domain.events";
    private String routingKeyPrefix = "";

    private PublisherConfig publisher = new PublisherConfig();
    private ConsumerConfig consumer = new ConsumerConfig();
    private DlqConfig dlq = new DlqConfig();
    private List<QueueConfig> queues = List.of();

    @Data
    public static class PublisherConfig {
        private boolean confirms = true;
        private boolean returns = true;
        private RetryConfig retry = new RetryConfig();
    }

    @Data
    public static class ConsumerConfig {
        private int prefetch = 10;
        private boolean defaultRequeueRejected = false;
        private ConcurrencyConfig concurrency = new ConcurrencyConfig();
    }

    @Data
    public static class DlqConfig {
        private boolean enabled = true;
        private String exchange = "eo.dlq";
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration initialInterval = Duration.ofSeconds(1);
        private double multiplier = 2.0;
    }

    @Data
    public static class ConcurrencyConfig {
        private int min = 1;
        private int max = 5;
    }

    @Data
    public static class QueueConfig {
        private String name;
        private String routingKeys;
        private boolean durable = true;
        private String type = "quorum";
    }
}
