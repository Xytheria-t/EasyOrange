package com.cartethyia.easyorange.framework.messaging.config;

import com.cartethyia.easyorange.framework.event.metadata.EventMetadataMessagePostProcessor;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "eo.domain.events";
    public static final String DLQ_EXCHANGE_NAME = "eo.dlq";
    public static final String TERMINAL_QUEUE = "eo.dlq.terminal";

    // Per-consumer queue names (used by @RabbitListener in consumer modules)
    public static final String QUEUE_PRODUCT_CQRS = "eo.product.cqrs";
    public static final String QUEUE_ORDER_NOTIFICATION = "eo.order.notification";
    public static final String QUEUE_ORDER_LIFECYCLE = "eo.order.lifecycle";
    public static final String QUEUE_AUDIT_NOTIFICATION = "eo.audit.notification";
    public static final String QUEUE_AUDIT_LOG = "eo.audit.log";
    public static final String QUEUE_REPORT_NOTIFICATION = "eo.report.notification";
    public static final String QUEUE_MESSAGE_WEBSOCKET = "eo.message.websocket";
    public static final String QUEUE_PAYMENT_METRICS = "eo.payment.metrics";

    // AI 事件驱动队列
    public static final String QUEUE_AI_PRODUCT = "eo.ai.product";
    public static final String QUEUE_AI_CREDIT = "eo.ai.credit";

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

        var specs = List.of(
                new QueueSpec(QUEUE_PRODUCT_CQRS, "product.#", "stock.#"),
                new QueueSpec(QUEUE_ORDER_NOTIFICATION, "order.#"),
                new QueueSpec(
                        QUEUE_ORDER_LIFECYCLE, "order.created", "order.cancelled", "order.completed", "order.refunded"),
                new QueueSpec(QUEUE_AUDIT_NOTIFICATION, "product.audited"),
                new QueueSpec(QUEUE_AUDIT_LOG, "audit.log"),
                new QueueSpec(QUEUE_REPORT_NOTIFICATION, "report.#"),
                new QueueSpec(QUEUE_MESSAGE_WEBSOCKET, "message.recalled"),
                new QueueSpec(QUEUE_PAYMENT_METRICS, "payment.#"),
                new QueueSpec(QUEUE_AI_PRODUCT, "product.created", "product.updated", "product.marked.sold"),
                new QueueSpec(QUEUE_AI_CREDIT, "order.completed", "report.processed"));

        for (var q : specs) {
            var queue = QueueBuilder.durable(q.name())
                    .quorum()
                    .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME)
                    .withArgument("x-dead-letter-routing-key", q.name() + ".dlq")
                    .build();
            var dlq = QueueBuilder.durable(q.name() + ".dlq").quorum().build();

            declarables.add(queue);
            declarables.add(dlq);
            declarables.add(BindingBuilder.bind(dlq).to(dlqExchange).with(q.name() + ".dlq"));

            for (var key : q.routingKeys()) {
                declarables.add(
                        BindingBuilder.bind(queue).to(domainEventExchange).with(key));
            }
        }

        // Terminal queue — 超过 max-retries 的毒消息转储，等待人工介入
        var terminalQueue = QueueBuilder.durable(TERMINAL_QUEUE).quorum().build();
        declarables.add(terminalQueue);

        log.info("Declared RabbitMQ topology: {} queues with DLQs and bindings, 1 terminal queue", specs.size());
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

    /**
     * RabbitTemplate 由 Boot 自动装配（连接 / 消息转换器 / 重试均由 {@code spring.rabbitmq.*} 属性驱动），
     * 这里仅通过 Customizer 补充投递侧定制：
     * mandatory 与 publisher confirm 由 {@code spring.rabbitmq.publisher-returns} / {@code publisher-confirm-type} 开启，
     * 本定制只负责元数据注入与失败告警；投递可靠性由 Modulith outbox（事务 + 重启重投）兜底。
     * <p>
     * bean 名避开 {@code rabbitTemplateCustomizer}：Modulith 的 RabbitJacksonConfiguration 已占用该名
     * （为事件外部化把 template 转换器换成 Jackson），Boot 会按序应用全部 RabbitTemplateCustomizer。
     */
    @Bean
    public RabbitTemplateCustomizer eoRabbitTemplateCustomizer(EventMetadataMessagePostProcessor metadataPostProcessor) {
        return template -> {
            // 投递前注入事件元数据（timestamp / traceId）到 message headers
            template.setBeforePublishPostProcessors(metadataPostProcessor);
            // 路由不到队列的消息显式告警，不静默丢失
            template.setReturnsCallback(returned -> log.error(
                    "消息路由失败（mandatory return）: exchange={} routingKey={} replyCode={} replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()));
            // publisher confirm 失败告警（correlated 模式，由 spring.rabbitmq.publisher-confirm-type 开启）
            template.setConfirmCallback((correlationData, ack, cause) -> {
                if (!ack) {
                    log.error("消息确认失败（nack）: cause={}", cause);
                }
            });
        };
    }
}
