package com.cartethyia.easyorange.framework.audit.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;

/**
 * 审计日志领域事件 — 通过 Spring Modulith Outbox 异步持久化审计记录。
 * <p>
 * 由 {@link com.cartethyia.easyorange.framework.audit.aspect.AuditLogAspect} 在写操作完成后发布，
 * 经 {@code EVENT_PUBLICATION} 表持久化（Outbox 模式）→ RabbitMQ 异步投递 →
 * {@link AuditLogEventConsumer} 消费写入 {@code eo_audit_log} 表。
 * <p>
 * 事件携带完整 {@link AuditLog} 实体（Jackson 序列化），消费者端重建实体并入库。
 * 聚合标识使用方法签名（{@code method}），因审计日志无业务聚合根。
 *
 * @param auditLog 审计日志实体（id 由消费者端 IdGenerator 生成）
 */
public record AuditLogEvent(String eventId, AuditLog auditLog) implements DomainEvent {

    public static AuditLogEvent of(AuditLog auditLog) {
        return new AuditLogEvent(UuidV7.generateId(), auditLog);
    }

    @Override
    public String aggregateId() {
        var method = auditLog.getMethod();
        return method != null ? method : "unknown";
    }
}
