package com.cartethyia.easyorange.framework.event.metadata;

import com.cartethyia.easyorange.common.event.DomainEvent;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;

/**
 * 领域事件元数据信封。
 * <p>
 * 由消费端 {@link com.cartethyia.easyorange.framework.event.core.EventConsumerHandler} 在 @RabbitHandler 调用时从 Message 与事件重建。
 * <p>
 * 字段语义：
 * <ul>
 *   <li>eventId: 事件唯一 ID (UUID v7)，取自事件实例自身（创建时生成，outbox/DLQ 重投不变），用于跨服务追踪与幂等去重</li>
 *   <li>occurredOn: 消息投递时刻（header，观测用途）</li>
 *   <li>eventType: 事件类型名 (取自 {@link DomainEvent#eventType()})</li>
 *   <li>aggregateId: 聚合根 ID</li>
 *   <li>traceId: Micrometer Tracing 的 traceId，跨服务串联</li>
 *   <li>causationId: 触发本事件的上游事件 ID，可选</li>
 * </ul>
 *
 * @param eventId     事件唯一 ID（事件实例携带；老消息兜底读 messageId header）
 * @param occurredOn  投递时刻
 * @param eventType   事件类型名
 * @param aggregateId 聚合根 ID
 * @param traceId     跨服务追踪 ID
 * @param causationId 因果链上游事件 ID (可空)
 */
public record EventMetadata(
        @Nullable String eventId,
        @Nullable Instant occurredOn,
        String eventType,
        String aggregateId,
        @Nullable String traceId,
        @Nullable String causationId) {

    private static final String HEADER_TRACE_ID = "traceId";
    private static final String HEADER_CAUSATION_ID = "causationId";

    /**
     * 从 RabbitMQ message headers 解码元数据，结合事件本身的属性。
     * <p>
     * eventId 优先取事件实例自身（重投稳定）；消息体未携带时（历史消息）兜底读 messageId header。
     */
    public static EventMetadata from(Message message, DomainEvent event) {
        var props = message.getMessageProperties();
        var eventId = event.eventId() != null ? event.eventId() : props.getMessageId();
        return new EventMetadata(
                eventId,
                props.getTimestamp() != null ? props.getTimestamp().toInstant() : null,
                event.eventType(),
                event.aggregateId(),
                props.getHeader(HEADER_TRACE_ID),
                props.getHeader(HEADER_CAUSATION_ID));
    }
}
