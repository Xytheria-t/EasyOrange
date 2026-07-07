package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RoutingKeyResolver routingKeyResolver;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = routingKeyResolver.resolve(event);

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event
            );

            log.debug("Published event: {} with routing key: {}",
                event.getClass().getSimpleName(), routingKey);
        } catch (Exception e) {
            log.error("Failed to publish event: {}, routing key: {}",
                event.getClass().getSimpleName(), routingKey, e);
            throw e;
        }
    }
}
