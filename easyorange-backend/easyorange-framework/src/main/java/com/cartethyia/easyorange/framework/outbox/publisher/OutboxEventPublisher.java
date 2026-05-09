package com.cartethyia.easyorange.framework.outbox.publisher;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.repository.OutboxRepository;
import com.cartethyia.easyorange.framework.outbox.util.OutboxEventUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final int BATCH_SIZE = 100;

    @Transactional(rollbackFor = Exception.class)
    public void storeEvent(BaseDomainEvent event, Long aggregateId) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxMessage message = OutboxMessage.builder()
                    .eventId(UUID.fromString(event.getEventId()))
                    .aggregateType(event.getAggregateType())
                    .aggregateId(aggregateId)
                    .eventType(event.getClass().getName())
                    .payload(payload)
                    .status(OutboxMessage.STATUS_PENDING)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(message);
            log.debug("事件已存储到 Outbox: eventId={}, eventType={}", event.getEventId(), event.getClass().getName());
        } catch (Exception e) {
            log.error("存储事件到 Outbox 失败: eventId={}", event.getEventId(), e);
            throw new RuntimeException("存储事件失败", e);
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        try {
            List<OutboxMessage> pendingEvents = outboxRepository.findPending(BATCH_SIZE);
            
            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("开始发布待处理事件，数量: {}", pendingEvents.size());

            for (OutboxMessage message : pendingEvents) {
                publishEvent(message);
            }
        } catch (Exception e) {
            log.error("发布待处理事件失败", e);
        }
    }

    private void publishEvent(OutboxMessage message) {
        try {
            BaseDomainEvent domainEvent = OutboxEventUtils.deserializeEvent(message, objectMapper);

            if (domainEvent == null) {
                log.warn("事件反序列化失败，跳过: eventId={}, eventType={}", message.getEventId(), message.getEventType());
                outboxRepository.markAsPublished(message.getEventId());
                return;
            }

            applicationEventPublisher.publishEvent(domainEvent);
            outboxRepository.markAsPublished(message.getEventId());

            log.info("事件发布成功 eventId={} eventType={}", message.getEventId(), message.getEventType());
        } catch (Exception e) {
            log.error("事件发布失败 eventId={}", message.getEventId(), e);
            outboxRepository.markAsFailed(message.getEventId(), OutboxEventUtils.truncate(e.getMessage()));
        }
    }
}
