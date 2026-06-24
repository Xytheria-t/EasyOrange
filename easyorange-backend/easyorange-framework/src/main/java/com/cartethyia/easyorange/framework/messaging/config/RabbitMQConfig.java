package com.cartethyia.easyorange.framework.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "eo.domain.events";
    public static final String DLQ_EXCHANGE_NAME = "eo.dlq";

    // Per-consumer queue names
    public static final String QUEUE_PRODUCT_CQRS = "eo.product.cqrs";
    public static final String QUEUE_ORDER_NOTIFICATION = "eo.order.notification";
    public static final String QUEUE_ORDER_SAGA = "eo.order.saga";
    public static final String QUEUE_STOCK_RESERVATION = "eo.stock.reservation";
    public static final String QUEUE_PAYMENT_INITIATION = "eo.payment.initiation";
    public static final String QUEUE_AUDIT_NOTIFICATION = "eo.audit.notification";
    public static final String QUEUE_REPORT_NOTIFICATION = "eo.report.notification";
    public static final String QUEUE_MESSAGE_WEBSOCKET = "eo.message.websocket";
    public static final String QUEUE_PAYMENT_METRICS = "eo.payment.metrics";
    public static final String QUEUE_OFFER_EVENTS = "eo.offer.events";

    private final RabbitMQProperties properties;

    // === Exchange ===

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE_NAME, true, false);
    }

    // === Queues (with DLQ arguments) ===

    @Bean
    public Queue productCqrsQueue() {
        return createQueue(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Queue orderNotificationQueue() {
        return createQueue(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Queue orderSagaQueue() {
        return createQueue(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Queue stockReservationQueue() {
        return createQueue(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Queue paymentInitiationQueue() {
        return createQueue(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Queue auditNotificationQueue() {
        return createQueue(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Queue reportNotificationQueue() {
        return createQueue(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Queue messageWebSocketQueue() {
        return createQueue(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Queue paymentMetricsQueue() {
        return createQueue(QUEUE_PAYMENT_METRICS);
    }

    @Bean
    public Queue offerEventsQueue() {
        return createQueue(QUEUE_OFFER_EVENTS);
    }

    // === DLQ Queues ===

    @Bean
    public Queue productCqrsDlq() {
        return createDlq(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Queue orderNotificationDlq() {
        return createDlq(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Queue orderSagaDlq() {
        return createDlq(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Queue stockReservationDlq() {
        return createDlq(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Queue paymentInitiationDlq() {
        return createDlq(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Queue auditNotificationDlq() {
        return createDlq(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Queue reportNotificationDlq() {
        return createDlq(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Queue messageWebSocketDlq() {
        return createDlq(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Queue paymentMetricsDlq() {
        return createDlq(QUEUE_PAYMENT_METRICS);
    }

    @Bean
    public Queue offerEventsDlq() {
        return createDlq(QUEUE_OFFER_EVENTS);
    }

    // === DLQ Bindings ===

    @Bean
    public Binding productCqrsDlqBinding() {
        return bindDlq(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Binding orderNotificationDlqBinding() {
        return bindDlq(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Binding orderSagaDlqBinding() {
        return bindDlq(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Binding stockReservationDlqBinding() {
        return bindDlq(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Binding paymentInitiationDlqBinding() {
        return bindDlq(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Binding auditNotificationDlqBinding() {
        return bindDlq(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Binding reportNotificationDlqBinding() {
        return bindDlq(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Binding messageWebSocketDlqBinding() {
        return bindDlq(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Binding paymentMetricsDlqBinding() {
        return bindDlq(QUEUE_PAYMENT_METRICS);
    }

    @Bean
    public Binding offerEventsDlqBinding() {
        return bindDlq(QUEUE_OFFER_EVENTS);
    }

    // === Event Bindings (queue → exchange → routing keys) ===

    // Product CQRS: product lifecycle + stock events
    @Bean
    public Binding productCqrsProductBinding(Queue productCqrsQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(productCqrsQueue).to(domainEventExchange).with("product.#");
    }

    @Bean
    public Binding productCqrsStockBinding(Queue productCqrsQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(productCqrsQueue).to(domainEventExchange).with("stock.#");
    }

    // Order notification: all order events
    @Bean
    public Binding orderNotificationBinding(Queue orderNotificationQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(orderNotificationQueue).to(domainEventExchange).with("order.#");
    }

    // Order saga: specific order lifecycle events
    @Bean
    public Binding orderSagaCreatedBinding(Queue orderSagaQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(orderSagaQueue).to(domainEventExchange).with("order.created");
    }

    @Bean
    public Binding orderSagaCancelledBinding(Queue orderSagaQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(orderSagaQueue).to(domainEventExchange).with("order.cancelled");
    }

    @Bean
    public Binding orderSagaCompletedBinding(Queue orderSagaQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(orderSagaQueue).to(domainEventExchange).with("order.completed");
    }

    @Bean
    public Binding orderSagaRefundedBinding(Queue orderSagaQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(orderSagaQueue).to(domainEventExchange).with("order.refunded");
    }

    // Stock reservation
    @Bean
    public Binding stockReservationBinding(Queue stockReservationQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(stockReservationQueue).to(domainEventExchange).with("stock.reservation.requested");
    }

    // Payment initiation
    @Bean
    public Binding paymentInitiationBinding(Queue paymentInitiationQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(paymentInitiationQueue).to(domainEventExchange).with("payment.initiation.requested");
    }

    // Audit notification
    @Bean
    public Binding auditNotificationBinding(Queue auditNotificationQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(auditNotificationQueue).to(domainEventExchange).with("product.audited");
    }

    // Report notification
    @Bean
    public Binding reportNotificationBinding(Queue reportNotificationQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(reportNotificationQueue).to(domainEventExchange).with("report.#");
    }

    // Message websocket
    @Bean
    public Binding messageWebSocketBinding(Queue messageWebSocketQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(messageWebSocketQueue).to(domainEventExchange).with("message.recalled");
    }

    // Payment metrics: all payment events
    @Bean
    public Binding paymentMetricsBinding(Queue paymentMetricsQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(paymentMetricsQueue).to(domainEventExchange).with("payment.#");
    }

    // Offer events: offer.accepted, offer.rejected, offer.countered
    @Bean
    public Binding offerEventsBinding(Queue offerEventsQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(offerEventsQueue).to(domainEventExchange).with("offer.*");
    }

    // === Infrastructure Beans ===

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
            .maxRetries(properties.getPublisher().getRetry().getMaxAttempts() - 1)
            .delay(properties.getPublisher().getRetry().getInitialInterval())
            .multiplier(properties.getPublisher().getRetry().getMultiplier())
            .maxDelay(Duration.ofMillis(10000))
            .build();
        return new RetryTemplate(retryPolicy);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory domainEventContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryTemplate retryTemplate) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setPrefetchCount(properties.getConsumer().getPrefetch());
        factory.setConcurrentConsumers(properties.getConsumer().getConcurrency().getMin());
        factory.setMaxConcurrentConsumers(properties.getConsumer().getConcurrency().getMax());
        factory.setDefaultRequeueRejected(properties.getConsumer().isDefaultRequeueRejected());
        factory.setRetryTemplate(retryTemplate);
        return factory;
    }

    // === Helper Methods ===

    private Queue createQueue(String name) {
        return QueueBuilder.durable(name)
            .quorum()
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME)
            .withArgument("x-dead-letter-routing-key", name + ".dlq")
            .build();
    }

    private Queue createDlq(String queueName) {
        return QueueBuilder.durable(queueName + ".dlq")
            .quorum()
            .build();
    }

    private Binding bindDlq(String queueName) {
        return BindingBuilder.bind(new Queue(queueName + ".dlq"))
            .to(new TopicExchange(DLQ_EXCHANGE_NAME))
            .with(queueName + ".dlq");
    }
}
