package com.cartethyia.easyorange.common.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent implements Serializable {

    private final String eventId;
    private final String aggregateType;
    private final int version;
    private final Instant occurredOn;

    protected BaseDomainEvent(Class<?> aggregateType) {
        this(aggregateType, 1);
    }

    protected BaseDomainEvent(Class<?> aggregateType, int version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateType = aggregateType.getSimpleName();
        this.version = version;
        this.occurredOn = Instant.now();
    }

    public abstract String eventType();
}