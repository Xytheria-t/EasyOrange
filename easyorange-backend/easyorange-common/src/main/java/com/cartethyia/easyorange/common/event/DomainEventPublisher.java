package com.cartethyia.easyorange.common.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
