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

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "eo.domain.events";

    private final RabbitMQProperties properties;

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue productEventQueue() {
        return QueueBuilder
            .durable("eo.product.events")
            .quorum()
            .build();
    }

    @Bean
    public Queue orderEventQueue() {
        return QueueBuilder
            .durable("eo.order.events")
            .quorum()
            .build();
    }

    @Bean
    public Queue paymentEventQueue() {
        return QueueBuilder
            .durable("eo.payment.events")
            .quorum()
            .build();
    }

    @Bean
    public Queue messageEventQueue() {
        return QueueBuilder
            .durable("eo.message.events")
            .quorum()
            .build();
    }

    @Bean
    public Binding productOrderEventBinding(Queue productEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(productEventQueue)
            .to(exchange)
            .with("order.#");
    }

    @Bean
    public Binding productPaymentEventBinding(Queue productEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(productEventQueue)
            .to(exchange)
            .with("payment.#");
    }

    @Bean
    public Binding orderProductEventBinding(Queue orderEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(orderEventQueue)
            .to(exchange)
            .with("product.#");
    }

    @Bean
    public Binding orderPaymentEventBinding(Queue orderEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(orderEventQueue)
            .to(exchange)
            .with("payment.#");
    }

    @Bean
    public Binding paymentOrderEventBinding(Queue paymentEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(paymentEventQueue)
            .to(exchange)
            .with("order.#");
    }

    @Bean
    public Binding messageOrderEventBinding(Queue messageEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(messageEventQueue)
            .to(exchange)
            .with("order.#");
    }

    @Bean
    public Binding messageProductEventBinding(Queue messageEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(messageEventQueue)
            .to(exchange)
            .with("product.#");
    }

    @Bean
    public Binding messagePaymentEventBinding(Queue messageEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(messageEventQueue)
            .to(exchange)
            .with("payment.#");
    }

    @Bean
    public Binding messageMessageEventBinding(Queue messageEventQueue, TopicExchange exchange) {
        return BindingBuilder
            .bind(messageEventQueue)
            .to(exchange)
            .with("message.#");
    }

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
    public SimpleRabbitListenerContainerFactory domainEventContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setPrefetchCount(properties.getConsumer().getPrefetch());
        factory.setConcurrentConsumers(properties.getConsumer().getConcurrency().getMin());
        factory.setMaxConcurrentConsumers(properties.getConsumer().getConcurrency().getMax());
        factory.setDefaultRequeueRejected(properties.getConsumer().isDefaultRequeueRejected());
        return factory;
    }
}
