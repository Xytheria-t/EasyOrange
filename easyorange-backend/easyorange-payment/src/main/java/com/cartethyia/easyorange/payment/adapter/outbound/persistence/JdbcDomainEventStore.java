package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.repository.OutboxRepository;
import com.cartethyia.easyorange.payment.domain.port.DomainEventStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcDomainEventStore implements DomainEventStorePort {

    private final OutboxRepository outboxRepository;

    @Override
    public void store(OutboxMessage event) {
        try {
            outboxRepository.save(event);
        } catch (DuplicateKeyException e) {
            log.warn("事件已存在，跳过重复存储: eventId={}", event.getEventId());
        }
    }

    @Override
    public List<OutboxMessage> findUnpublished(int limit) {
        return outboxRepository.findPending(limit);
    }

    @Override
    public List<OutboxMessage> findPendingEvents(int limit) {
        return findUnpublished(limit);
    }

    @Override
    public void markAsPublished(UUID eventId) {
        outboxRepository.markAsPublished(eventId);
    }

    @Override
    public void markAsFailed(UUID eventId, String errorMessage) {
        outboxRepository.markAsFailed(eventId, errorMessage);
    }
}
