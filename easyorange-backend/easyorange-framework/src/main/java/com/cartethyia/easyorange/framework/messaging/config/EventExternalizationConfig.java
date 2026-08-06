package com.cartethyia.easyorange.framework.messaging.config;

import com.cartethyia.easyorange.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * Configures Spring Modulith's event externalization to RabbitMQ.
 * <p>
 * All domain events implementing {@link DomainEvent} are externalized to the
 * {@value RabbitMQConfig#EXCHANGE_NAME} Topic Exchange. The routing key is derived from
 * the event class name by convention: CamelCase → dot.case lowercase
 * (e.g. {@code OrderCreatedEvent} → {@code order.created}).
 * <p>
 * This config is active when RabbitMQ is enabled (default). When disabled,
 * events are still persisted in the {@code EVENT_PUBLICATION} table but never
 * externalized — useful for local development without RabbitMQ.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventExternalizationConfig {

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(DomainEvent.class)
                .route(
                        DomainEvent.class,
                        event -> RoutingTarget.forTarget(RabbitMQConfig.EXCHANGE_NAME)
                                .andKey(toRoutingKey(event)))
                .build();
    }

    /**
     * Converts a domain event class name to a RabbitMQ routing key.
     * Convention: CamelCase → dot.case lowercase, with trailing "Event" suffix stripped.
     * <p>
     * Examples:
     * <pre>
     *   ProductCreatedEvent  → "product.created"
     *   OrderPaidEvent      → "order.paid"
     * </pre>
     */
    private static String toRoutingKey(DomainEvent event) {
        String typeName = event.eventType();
        return typeName.replaceAll("([a-z])([A-Z0-9])", "$1.$2")
                .replaceAll("([0-9])([A-Z])", "$1.$2")
                .toLowerCase();
    }
}
