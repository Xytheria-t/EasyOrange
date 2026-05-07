package com.cartethyia.easyorange.payment.application.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.payment.domain.port.output.DomainEventStorePort;
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
public class DomainEventCompensator {

    private final DomainEventStorePort eventStore;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void compensateUnpublishedEvents() {
        List<StoredEvent> unpublished = eventStore.findUnpublished(BATCH_SIZE);
        if (unpublished.isEmpty()) {
            return;
        }

        log.info("发现 {} 条未发布领域事件，开始补偿", unpublished.size());
        for (StoredEvent event : unpublished) {
            try {
                BaseDomainEvent domainEvent = deserializeEvent(event);
                if (domainEvent != null) {
                    domainEventPublisher.publish(domainEvent);
                    eventStore.markAsPublished(event.getEventId());
                    log.info("补偿发布领域事件成功: eventType={} eventId={}", event.getEventType(), event.getEventId());
                } else {
                    log.warn("补偿发布领域事件跳过: 无法反序列化 eventType={} eventId={}", event.getEventType(), event.getEventId());
                    eventStore.markAsPublished(event.getEventId());
                }
            } catch (Exception e) {
                log.error("补偿发布领域事件失败: eventType={} eventId={}", event.getEventType(), event.getEventId(), e);
            }
        }
    }

    private BaseDomainEvent deserializeEvent(StoredEvent storedEvent) {
        try {
            Class<?> eventClass = Class.forName(storedEvent.getEventType());
            if (BaseDomainEvent.class.isAssignableFrom(eventClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends BaseDomainEvent> clazz = (Class<? extends BaseDomainEvent>) eventClass;
                return objectMapper.readValue(storedEvent.getPayload(), clazz);
            }
        } catch (ClassNotFoundException e) {
            log.warn("领域事件类未找到: eventType={}", storedEvent.getEventType());
        } catch (Exception e) {
            log.error("领域事件反序列化失败: eventType={} eventId={}", storedEvent.getEventType(), storedEvent.getEventId(), e);
        }
        return null;
    }
}
