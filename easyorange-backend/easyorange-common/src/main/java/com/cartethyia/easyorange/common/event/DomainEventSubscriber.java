package com.cartethyia.easyorange.common.event;

public interface DomainEventSubscriber {

    Class<? extends BaseDomainEvent> getEventType();

    void handle(BaseDomainEvent event);
}
