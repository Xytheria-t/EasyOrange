package com.cartethyia.easyorange.payment.domain.port.output;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import java.util.List;
import java.util.UUID;

public interface DomainEventStorePort {
    void store(OutboxMessage event);
    List<OutboxMessage> findUnpublished(int limit);
    List<OutboxMessage> findPendingEvents(int limit);
    void markAsPublished(UUID eventId);
    void markAsFailed(UUID eventId, String errorMessage);
}
