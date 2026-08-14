package com.cartethyia.easyorange.framework.event.metadata;

import java.time.Instant;
import java.util.Date;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * RabbitMQ 消息发布前注入投递元数据。
 * <p>
 * 注入字段：
 * <ul>
 *   <li>timestamp: 当前投递时刻（header 仅作观测；事件发生时刻请以事件载荷为准）</li>
 *   <li>traceId header: 从 MDC 复制（由 Micrometer Tracing 注入）</li>
 * </ul>
 * <p>
 * 注意：不注入 eventId —— 事件唯一 ID 由事件实例自身携带（{@code DomainEvent.eventId()}，创建时生成），
 * outbox 重投 / DLQ 重投时保持不变，消费端幂等去重依赖它而非投递期生成的 header。
 * <p>
 * 由 {@code RabbitMQConfig.rabbitTemplate()} 注册为 {@code beforePublishPostProcessor}。
 */
@NullMarked
public class EventMetadataMessagePostProcessor implements MessagePostProcessor {

    public static final String HEADER_TRACE_ID = "traceId";

    @Override
    public Message postProcessMessage(Message message) {
        var props = message.getMessageProperties();
        props.setTimestamp(Date.from(Instant.now()));

        var traceId = MDC.get("traceId");
        if (traceId != null) {
            props.setHeader(HEADER_TRACE_ID, traceId);
        }
        return message;
    }
}
