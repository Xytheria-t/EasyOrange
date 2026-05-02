package com.cartethyia.easyorange.payment.application.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.payment.domain.event.DomainEventStore;
import com.cartethyia.easyorange.payment.domain.event.StoredEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final DomainEventStore eventStore;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        try {
            List<StoredEvent> pendingEvents = eventStore.findPendingEvents(BATCH_SIZE);
            
            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("开始发布待处理事件，数量: {}", pendingEvents.size());

            for (StoredEvent storedEvent : pendingEvents) {
                publishEvent(storedEvent);
            }
        } catch (Exception e) {
            log.error("发布待处理事件失败", e);
        }
    }

    private void publishEvent(StoredEvent storedEvent) {
        try {
            BaseDomainEvent domainEvent = deserializeEvent(storedEvent);
            
            domainEventPublisher.publish(domainEvent);
            
            eventStore.markAsPublished(storedEvent.getEventId());
            
            log.info("事件发布成功 eventId={} eventType={}", 
                storedEvent.getEventId(), storedEvent.getEventType());
        } catch (Exception e) {
            log.error("事件发布失败 eventId={}", storedEvent.getEventId(), e);
            
            eventStore.markAsFailed(storedEvent.getEventId(), e.getMessage());
        }
    }

    private BaseDomainEvent deserializeEvent(StoredEvent storedEvent) throws Exception {
        Class<?> eventClass = Class.forName(storedEvent.getEventType());
        return (BaseDomainEvent) objectMapper.readValue(storedEvent.getPayload(), eventClass);
    }
}
