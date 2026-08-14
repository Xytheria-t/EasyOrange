package com.cartethyia.easyorange.framework.audit.event;

import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 审计日志事件消费者 — 接收 {@link AuditLogEvent}，将审计记录写入 {@code eo_audit_log} 表。
 * <p>
 * 事件流：{@code AuditLogAspect} → Spring Modulith Outbox（{@code EVENT_PUBLICATION} 表）
 * → RabbitMQ → 本消费者 → {@link AuditLogService} 入库。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_AUDIT_LOG, containerFactory = "domainEventContainerFactory")
public class AuditLogEventConsumer {

    private final EventConsumerHandler handler;
    private final AuditLogService auditLogService;

    public AuditLogEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            AuditLogService auditLogService) {
        // 审计日志不做幂等去重：审计允许重复写，但「先标记后处理」的瞬断可能把未处理事件
        // 短路成重复跳过而静默丢失记录——重复一条审计可接受，丢失不可接受
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService, false);
        this.auditLogService = auditLogService;
    }

    @RabbitHandler
    public void onAuditLog(AuditLogEvent event, Message message) {
        handler.handle(event, message, () -> {
            var auditLog = event.auditLog();
            log.info(
                    "审计日志入库: method={} status={} user={} ip={} duration={}ms",
                    auditLog.getMethod(),
                    auditLog.getStatus(),
                    auditLog.getUsername(),
                    auditLog.getClientIp(),
                    auditLog.getDuration());
            auditLogService.insertAuditLog(auditLog);
        });
    }
}
