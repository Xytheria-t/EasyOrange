package com.cartethyia.easyorange.common.ddd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID extends ValueObject> {

    private final List<DomainEventRecord> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEventRecord event) {
        domainEvents.add(event);
    }

    public List<DomainEventRecord> releaseEvents() {
        List<DomainEventRecord> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean hasEvents() {
        return !domainEvents.isEmpty();
    }

    public abstract ID id();

    public static class DomainEventRecord {
        private final Object event;
        private final long timestamp;

        public DomainEventRecord(Object event) {
            this.event = event;
            this.timestamp = System.currentTimeMillis();
        }

        public Object getEvent() {
            return event;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
