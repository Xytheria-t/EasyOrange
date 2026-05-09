package com.cartethyia.easyorange.common.event;

public interface OutboxDomainEventPublisher {

    void publishWithOutbox(BaseDomainEvent event, Long aggregateId);

    void publishAllWithOutbox(java.util.List<BaseDomainEvent> events, Long aggregateId);
}
