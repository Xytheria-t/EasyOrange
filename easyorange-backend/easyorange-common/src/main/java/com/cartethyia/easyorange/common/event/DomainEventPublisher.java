package com.cartethyia.easyorange.common.event;

public interface DomainEventPublisher {

    void publish(BaseDomainEvent event);
}