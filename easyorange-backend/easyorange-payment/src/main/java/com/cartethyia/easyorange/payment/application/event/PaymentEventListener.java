package com.cartethyia.easyorange.payment.application.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.payment.domain.port.DomainEventStorePort;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final DomainEventStorePort eventStore;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void onPaymentCreated(PaymentCreatedEvent event) {
        persistEvent(event, event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        persistEvent(event, event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        persistEvent(event, event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        persistEvent(event, event.getPaymentId());
    }

    @Async
    @EventListener
    public void onPaymentClosed(PaymentClosedEvent event) {
        persistEvent(event, event.getPaymentId());
    }

    private void persistEvent(BaseDomainEvent event, Long aggregateId) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .eventId(UUID.fromString(event.getEventId()))
                    .aggregateType(event.getAggregateType())
                    .aggregateId(aggregateId)
                    .eventType(event.getClass().getName())
                    .payload(payload)
                    .status(OutboxMessage.STATUS_PENDING)
                    .createdAt(event.getOccurredOn() != null ? event.getOccurredOn() : Instant.now())
                    .build();
            eventStore.store(outboxMessage);
        } catch (JacksonException e) {
            log.error("领域事件持久化失败 eventType={} eventId={}", event.eventType(), event.getEventId(), e);
        }
    }
}
