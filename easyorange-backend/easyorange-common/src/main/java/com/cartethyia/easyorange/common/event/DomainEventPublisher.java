package com.cartethyia.easyorange.common.event;

import java.util.List;

public interface DomainEventPublisher {

    void publish(BaseDomainEvent event);

    void publishAll(List<BaseDomainEvent> events);
}