package com.cartethyia.easyorange.framework.outbox.publisher;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.OutboxDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDomainEventPublisherImpl implements OutboxDomainEventPublisher {

    private final OutboxEventPublisher outboxEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishWithOutbox(BaseDomainEvent event, Long aggregateId) {
        outboxEventPublisher.storeEvent(event, aggregateId);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAllWithOutbox(List<BaseDomainEvent> events, Long aggregateId) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (BaseDomainEvent event : events) {
            publishWithOutbox(event, aggregateId);
        }
    }
}
