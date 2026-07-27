package com.cartethyia.easyorange.framework.audit.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
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
 * <p>
 * 继承 {@link AbstractDomainEventConsumer} 模板，复用幂等检查 + Micrometer 指标 + 结构化日志 + DLQ。
 * 审计日志为通知类消费者（无需业务幂等），构造器传 {@code idempotencyEnabled=false} 关闭幂等检查，
 * 避免同一操作多次审计被误判为重复而跳过。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_AUDIT_LOG, containerFactory = "domainEventContainerFactory")
public class AuditLogEventConsumer extends AbstractDomainEventConsumer {

    private final AuditLogService auditLogService;

    public AuditLogEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                  EventMetricsService metricsService,
                                  AuditLogService auditLogService) {
        super(idempotencyChecker, metricsService, false);
        this.auditLogService = auditLogService;
    }

    @RabbitHandler
    public void onAuditLog(AuditLogEvent event, Message message) {
        handle(event, message);
    }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        if (!(event instanceof AuditLogEvent e)) {
            throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
        AuditLog auditLog = e.auditLog();
        log.info("审计日志入库: method={} status={} user={} ip={} duration={}ms",
                auditLog.getMethod(),
                auditLog.getStatus(),
                auditLog.getUsername(),
                auditLog.getClientIp(),
                auditLog.getDuration());
        auditLogService.insertAuditLog(auditLog);
    }
}
