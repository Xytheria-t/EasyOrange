package com.cartethyia.easyorange.common.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final String aggregateType;
    private final Instant occurredOn;

    protected BaseDomainEvent(String aggregateType) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.occurredOn = Instant.now();
    }

    public abstract String eventType();
}
