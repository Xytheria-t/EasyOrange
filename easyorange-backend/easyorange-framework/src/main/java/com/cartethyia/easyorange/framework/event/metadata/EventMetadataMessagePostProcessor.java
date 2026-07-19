package com.cartethyia.easyorange.framework.event.metadata;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * RabbitMQ 消息发布前注入事件元数据。
 * <p>
 * 注入字段：
 * <ul>
 *   <li>messageId: UUID（事件唯一 ID）</li>
 *   <li>timestamp: 当前时刻</li>
 *   <li>traceId header: 从 MDC 复制（由 Micrometer Tracing 注入）</li>
 * </ul>
 * <p>
 * 由 {@code RabbitMQConfig.rabbitTemplate()} 注册为 {@code beforePublishPostProcessor}，
 * 所有经 Modulith 外化到 RabbitMQ 的领域事件自动获得元数据。
 */
public class EventMetadataMessagePostProcessor implements MessagePostProcessor {

    public static final String HEADER_TRACE_ID = "traceId";
    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_OCCURRED_ON = "occurredOn";

    @Override
    public Message postProcessMessage(Message message) {
        var props = message.getMessageProperties();
        var eventId = UUID.randomUUID().toString();
        var now = Instant.now();
        props.setMessageId(eventId);
        props.setTimestamp(Date.from(now));
        props.setHeader(HEADER_EVENT_ID, eventId);
        props.setHeader(HEADER_OCCURRED_ON, now.toString());

        var traceId = MDC.get("traceId");
        if (traceId != null) {
            props.setHeader(HEADER_TRACE_ID, traceId);
        }
        return message;
    }
}
