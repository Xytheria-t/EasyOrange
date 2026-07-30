package com.cartethyia.easyorange.framework.event.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
public final class EventConsumerHandler {

    private final String consumerId;
    private final EventIdempotencyChecker idempotencyChecker;
    private final EventMetricsService metricsService;
    private final boolean idempotencyEnabled;

    public EventConsumerHandler(String consumerId,
                                 EventIdempotencyChecker idempotencyChecker,
                                 EventMetricsService metricsService) {
        this(consumerId, idempotencyChecker, metricsService, true);
    }

    public void handle(DomainEvent event, Message message, Consumer<EventMetadata> consumer) {
        var metadata = EventMetadata.from(message, event);
        var sample = metricsService.startTimer();
        var outcome = "success";
        try {
            var eventId = metadata.eventId();
            if (idempotencyEnabled && eventId != null && isDuplicate(event.eventType(), eventId)) {
                log.info("重复跳过: type={} aggregateId={} eventId={} consumer={}",
                        event.eventType(), event.aggregateId(), eventId, consumerId);
                outcome = "duplicate";
                return;
            }
            log.info("开始处理: type={} aggregateId={} eventId={} traceId={} consumer={}",
                    event.eventType(), event.aggregateId(), eventId, metadata.traceId(), consumerId);
            consumer.accept(metadata);
            log.info("完成处理: type={} aggregateId={} consumer={}",
                    event.eventType(), event.aggregateId(), consumerId);
        } catch (Exception e) {
            outcome = "failure";
            log.error("处理失败: type={} aggregateId={} eventId={} consumer={}",
                    event.eventType(), event.aggregateId(), metadata.eventId(), consumerId, e);
            throw e;
        } finally {
            metricsService.recordReceived(event.eventType(), outcome);
            metricsService.recordDuration(event.eventType(), sample, outcome);
        }
    }

    private boolean isDuplicate(String eventType, String eventId) {
        var namespace = consumerId + ":" + eventType;
        return idempotencyChecker.isDuplicate(namespace, eventId)
            || !idempotencyChecker.tryMark(namespace, eventId);
    }
}