package com.cartethyia.easyorange.payment.domain.event;

import java.util.List;
import java.util.UUID;

public interface DomainEventStore {

    void store(StoredEvent event);

    List<StoredEvent> findUnpublished(int limit);

    void markAsPublished(UUID eventId);
}
