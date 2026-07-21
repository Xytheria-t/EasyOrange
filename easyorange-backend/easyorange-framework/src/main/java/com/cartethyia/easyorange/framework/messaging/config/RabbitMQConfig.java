package com.cartethyia.easyorange.framework.messaging.config;

import com.cartethyia.easyorange.framework.event.metadata.EventMetadataMessagePostProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class  RabbitMQConfig {

    public static final String EXCHANGE_NAME = "eo.domain.events";
    public static final String DLQ_EXCHANGE_NAME = "eo.dlq";

    // Per-consumer queue names (used by @RabbitListener in consumer modules)
    public static final String QUEUE_PRODUCT_CQRS = "eo.product.cqrs";
    public static final String QUEUE_ORDER_NOTIFICATION = "eo.order.notification";
    public static final String QUEUE_ORDER_SAGA = "eo.order.saga";
    public static final String QUEUE_STOCK_RESERVATION = "eo.stock.reservation";
    public static final String QUEUE_AUDIT_NOTIFICATION = "eo.audit.notification";
    public static final String QUEUE_REPORT_NOTIFICATION = "eo.report.notification";
    public static final String QUEUE_MESSAGE_WEBSOCKET = "eo.message.websocket";
    public static final String QUEUE_PAYMENT_METRICS = "eo.payment.metrics";

    // AI 事件驱动队列
    public static final String QUEUE_AI_PRODUCT = "eo.ai.product";
    public static final String QUEUE_AI_CREDIT = "eo.ai.credit";
    public static final String QUEUE_COMPENSATION_ALERT = "eo.compensation.alert";

    private final RabbitMQProperties properties;

    // === Exchanges ===

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE_NAME, true, false);
    }

    // === Topology: queues, DLQs, and bindings (single Declarables eliminates ~40 individual @Bean methods) ===

    @Bean
    public Declarables domainEventTopology(TopicExchange domainEventExchange, TopicExchange dlqExchange) {
        var declarables = new ArrayList<Declarable>();

        record QueueSpec(String name, String... routingKeys) {}

        for (var q : List.of(
                new QueueSpec(QUEUE_PRODUCT_CQRS, "product.#", "stock.#"),
                new QueueSpec(QUEUE_ORDER_NOTIFICATION, "order.#"),
                new QueueSpec(QUEUE_ORDER_SAGA, "order.created", "order.cancelled", "order.completed", "order.refunded"),
                new QueueSpec(QUEUE_STOCK_RESERVATION, "stock.reservation.requested"),
                new QueueSpec(QUEUE_AUDIT_NOTIFICATION, "product.audited"),
                new QueueSpec(QUEUE_REPORT_NOTIFICATION, "report.#"),
                new QueueSpec(QUEUE_MESSAGE_WEBSOCKET, "message.recalled"),
                new QueueSpec(QUEUE_PAYMENT_METRICS, "payment.#", "compensation.failed.alert"),
                new QueueSpec(QUEUE_AI_PRODUCT, "product.created", "product.updated", "product.marked.sold"),
                new QueueSpec(QUEUE_AI_CREDIT, "order.completed", "report.processed"),
                new QueueSpec(QUEUE_COMPENSATION_ALERT, "compensation.failed.alert")
        )) {
            var queue = QueueBuilder.durable(q.name())
                    .quorum()
                    .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME)
                    .withArgument("x-dead-letter-routing-key", q.name() + ".dlq")
                    .build();
            var dlq = QueueBuilder.durable(q.name() + ".dlq")
                    .quorum()
                    .build();

            declarables.add(queue);
            declarables.add(dlq);
            declarables.add(BindingBuilder.bind(dlq).to(dlqExchange).with(q.name() + ".dlq"));

            for (var key : q.routingKeys()) {
                declarables.add(BindingBuilder.bind(queue).to(domainEventExchange).with(key));
            }
        }

        log.info("Declared RabbitMQ topology: {} queues with DLQs and bindings", 11);
        return new Declarables(declarables);
    }

    // === Infrastructure Beans ===

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public EventMetadataMessagePostProcessor eventMetadataMessagePostProcessor() {
        return new EventMetadataMessagePostProcessor();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter,
                                         EventMetadataMessagePostProcessor metadataPostProcessor) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        // 发布前注入事件元数据（eventId / timestamp / traceId）到 message headers
        template.setBeforePublishPostProcessors(metadataPostProcessor);
        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        var cfg = properties.getPublisher().getRetry();
        var policy = RetryPolicy.builder()
                .maxRetries(cfg.getMaxAttempts() - 1)
                .delay(cfg.getInitialInterval())
                .multiplier(cfg.getMultiplier())
                .maxDelay(Duration.ofMillis(10000))
                .build();
        return new RetryTemplate(policy);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory domainEventContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryTemplate retryTemplate) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setPrefetchCount(properties.getConsumer().getPrefetch());
        factory.setConcurrentConsumers(properties.getConsumer().getConcurrency().getMin());
        factory.setMaxConcurrentConsumers(properties.getConsumer().getConcurrency().getMax());
        factory.setDefaultRequeueRejected(properties.getConsumer().isDefaultRequeueRejected());
        factory.setRetryTemplate(retryTemplate);
        return factory;
    }
}
