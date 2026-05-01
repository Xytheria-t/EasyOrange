package com.cartethyia.easyorange.framework.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final DomainEventPersistenceService persistenceService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(BaseDomainEvent event) {
        log.info("发布领域事件：type={} eventId={}", event.eventType(), event.getEventId());
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
        log.info("异步发布领域事件：type={} eventId={}", event.eventType(), event.getEventId());
        persistenceService.persist(event);
        applicationEventPublisher.publishEvent(event);
    }
}