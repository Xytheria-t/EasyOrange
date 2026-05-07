package com.cartethyia.easyorange.payment.domain.port.output;

import java.util.List;
import java.util.UUID;

import com.cartethyia.easyorange.payment.domain.event.StoredEvent;

public interface DomainEventStorePort {

    void store(StoredEvent event);

    List<StoredEvent> findUnpublished(int limit);

    List<StoredEvent> findPendingEvents(int limit);

    void markAsPublished(UUID eventId);

    void markAsFailed(UUID eventId, String errorMessage);
}
