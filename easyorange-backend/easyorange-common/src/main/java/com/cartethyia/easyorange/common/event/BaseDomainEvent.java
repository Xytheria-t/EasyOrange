package com.cartethyia.easyorange.common.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent implements Serializable {

    private final String eventId;
    private final Instant occurredOn;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
    }

    public String eventType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName;
    }
}
