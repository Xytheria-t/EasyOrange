package com.cartethyia.easyorange.common.event;

public interface DomainEventSubscriber<T extends BaseDomainEvent> {

    Class<T> getEventType();

    void handle(T event);

    default int order() {
        return 0;
    }
}
