package com.cartethyia.easyorange.framework.outbox.converter;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessagePO;

import java.time.Instant;

public final class OutboxMessageConverter {

    private OutboxMessageConverter() {}

    public static OutboxMessage toDomain(OutboxMessagePO po) {
        return OutboxMessage.builder()
                .eventId(po.getEventId())
                .aggregateType(po.getAggregateType())
                .aggregateId(po.getAggregateId())
                .eventType(po.getEventType())
                .payload(po.getPayload())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .publishedAt(po.getPublishedAt())
                .errorMessage(po.getErrorMessage())
                .build();
    }

    public static OutboxMessagePO toPO(OutboxMessage message) {
        return OutboxMessagePO.builder()
                .eventId(message.getEventId())
                .aggregateType(message.getAggregateType())
                .aggregateId(message.getAggregateId())
                .eventType(message.getEventType())
                .payload(message.getPayload())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now())
                .publishedAt(message.getPublishedAt())
                .errorMessage(message.getErrorMessage())
                .build();
    }
}
