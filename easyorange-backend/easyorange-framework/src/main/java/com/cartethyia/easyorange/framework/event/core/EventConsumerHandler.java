package com.cartethyia.easyorange.framework.event.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

@Slf4j
@AllArgsConstructor
public final class EventConsumerHandler {

    private final String consumerId;
    private final EventIdempotencyChecker idempotencyChecker;
    private final EventMetricsService metricsService;
    private final boolean idempotencyEnabled;

    public EventConsumerHandler(
            String consumerId, EventIdempotencyChecker idempotencyChecker, EventMetricsService metricsService) {
        this(consumerId, idempotencyChecker, metricsService, true);
    }

    public void handle(DomainEvent event, Message message, Consumer<EventMetadata> consumer) {
        var metadata = EventMetadata.from(message, event);
        var eventId = metadata.eventId();
        var namespace = consumerId + ":" + event.eventType();
        // 把 RabbitMQ 头里透传的 traceId 回填 MDC，使消费处理及其下游（@Async、缓存失效等）日志继承同一链路
        var previousMdc = MDC.getCopyOfContextMap();
        var messageTraceId = metadata.traceId();
        if (messageTraceId != null && !messageTraceId.isBlank()) {
            MDC.put("traceId", messageTraceId);
        }
        var sample = metricsService.startTimer();
        var outcome = "success";
        try {
            // 原子领取处理权：tryMark = SET NX EX，返回 false 表示重复（已处理或他人处理中）
            if (idempotencyEnabled && eventId != null && !idempotencyChecker.tryMark(namespace, eventId)) {
                log.info(
                        "重复跳过: type={} aggregateId={} eventId={} consumer={}",
                        event.eventType(),
                        event.aggregateId(),
                        eventId,
                        consumerId);
                outcome = "duplicate";
                return;
            }
            log.info(
                    "开始处理: type={} aggregateId={} eventId={} traceId={} consumer={}",
                    event.eventType(),
                    event.aggregateId(),
                    eventId,
                    messageTraceId,
                    consumerId);
            consumer.accept(metadata);
            log.info("完成处理: type={} aggregateId={} consumer={}", event.eventType(), event.aggregateId(), consumerId);
        } catch (Exception e) {
            outcome = "failure";
            log.error(
                    "处理失败: type={} aggregateId={} eventId={} consumer={}",
                    event.eventType(),
                    event.aggregateId(),
                    eventId,
                    consumerId,
                    e);
            // 撤销幂等标记，使容器重试 / DLQ 重投能够重新执行；否则「先标记后处理」会把瞬时失败
            // 短路成重复跳过而静默丢失事件
            if (idempotencyEnabled && eventId != null) {
                idempotencyChecker.unmark(namespace, eventId);
            }
            throw e;
        } finally {
            metricsService.recordReceived(event.eventType(), outcome);
            metricsService.recordDuration(event.eventType(), sample, outcome);
            restoreMdc(previousMdc);
        }
    }

    /** 恢复消费线程进入前的 MDC，避免 traceId 污染复用线程的后续任务。 */
    private void restoreMdc(Map<String, String> previousMdc) {
        if (previousMdc == null || previousMdc.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousMdc);
        }
    }
}
