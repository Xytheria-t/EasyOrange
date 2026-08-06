package com.cartethyia.easyorange.framework.event.metadata;

import com.cartethyia.easyorange.common.event.DomainEvent;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;

/**
 * 领域事件元数据信封。
 * <p>
 * 由发布端 {@code EventMetadataMessagePostProcessor} 在 RabbitTemplate 发送前注入到 message headers，
 * 由消费端 {@link com.cartethyia.easyorange.framework.event.core.EventConsumerHandler} 在 @RabbitHandler 调用时从 Message 重建。
 * <p>
 * 字段语义：
 * <ul>
 *   <li>eventId: 事件唯一 ID (UUID v7)，用于跨服务追踪与幂等去重</li>
 *   <li>occurredOn: 事件发布时刻</li>
 *   <li>eventType: 事件类型名 (取自 {@link DomainEvent#eventType()})</li>
 *   <li>aggregateId: 聚合根 ID</li>
 *   <li>traceId: Micrometer Tracing 的 traceId，跨服务串联</li>
 *   <li>causationId: 触发本事件的上游事件 ID，可选</li>
 * </ul>
 *
 * @param eventId     事件唯一 ID
 * @param occurredOn  发生时刻
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
     */
    public static EventMetadata from(Message message, DomainEvent event) {
        var props = message.getMessageProperties();
        return new EventMetadata(
                props.getMessageId(),
                props.getTimestamp() != null ? props.getTimestamp().toInstant() : null,
                event.eventType(),
                event.aggregateId(),
                props.getHeader(HEADER_TRACE_ID),
                props.getHeader(HEADER_CAUSATION_ID));
    }
}
