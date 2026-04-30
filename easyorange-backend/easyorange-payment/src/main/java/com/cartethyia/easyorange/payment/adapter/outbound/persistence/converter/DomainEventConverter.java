package com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.DomainEventPO;
import com.cartethyia.easyorange.payment.domain.event.StoredEvent;

import java.time.Instant;

public final class DomainEventConverter {

    private DomainEventConverter() {}

    public static StoredEvent toStoredEvent(DomainEventPO po) {
        return StoredEvent.builder()
                .eventId(po.getEventId())
                .aggregateType(po.getAggregateType())
                .aggregateId(po.getAggregateId())
                .eventType(po.getEventType())
                .payload(po.getPayload())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .publishedAt(po.getPublishedAt())
                .build();
    }

    public static DomainEventPO toPO(StoredEvent event) {
        return DomainEventPO.builder()
                .eventId(event.getEventId())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now())
                .publishedAt(event.getPublishedAt())
                .build();
    }
}
