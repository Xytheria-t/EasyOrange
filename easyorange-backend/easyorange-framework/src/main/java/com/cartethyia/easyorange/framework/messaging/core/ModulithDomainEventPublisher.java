package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring Modulith-backed domain event publisher.
 * <p>
 * Delegates to {@link ApplicationEventPublisher}, which is intercepted by
 * Spring Modulith's Event Publication Registry. The registry writes the event
 * to the {@code EVENT_PUBLICATION} table in the same DB transaction as the
 * caller, then asynchronously externalizes it to RabbitMQ after commit.
 * <p>
 * This provides at-least-once delivery guarantee — if the application crashes
 * after the DB transaction commits but before RabbitMQ receives the message,
 * the event will be retried on restart (see
 * {@code spring.modulith.events.republish-outstanding-events-on-restart}).
 * <p>
 * Replaces {@link RabbitMQDomainEventPublisher} as the {@code @Primary}
 * implementation of {@link DomainEventPublisher}.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ModulithDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event via Modulith: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
        // Modulith intercepts this and:
        //   1. Writes EVENT_PUBLICATION row in the current DB transaction
        //   2. After tx commit, async externalizer forwards to RabbitMQ
        //   3. On failure, leaves the row in PUBLISHED state for retry
    }
}
