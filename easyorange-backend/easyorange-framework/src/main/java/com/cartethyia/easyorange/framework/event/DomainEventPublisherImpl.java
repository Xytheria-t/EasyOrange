package com.cartethyia.easyorange.framework.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final DomainEventPersistenceService persistenceService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(BaseDomainEvent event) {
        persistenceService.persist(event);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<BaseDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (BaseDomainEvent event : events) {
            publish(event);
        }
    }

    @Async("domainEventExecutor")
    public void publishAsync(BaseDomainEvent event) {
        persistenceService.persist(event);
        applicationEventPublisher.publishEvent(event);
    }
}