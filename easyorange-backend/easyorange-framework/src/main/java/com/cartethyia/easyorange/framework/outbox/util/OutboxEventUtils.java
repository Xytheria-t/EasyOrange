package com.cartethyia.easyorange.framework.outbox.util;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OutboxEventUtils {

    private OutboxEventUtils() {}

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    public static BaseDomainEvent deserializeEvent(OutboxMessage message, ObjectMapper objectMapper) {
        try {
            Class<?> eventClass = Class.forName(message.getEventType());
            if (BaseDomainEvent.class.isAssignableFrom(eventClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends BaseDomainEvent> clazz = (Class<? extends BaseDomainEvent>) eventClass;
                return objectMapper.readValue(message.getPayload(), clazz);
            }
        } catch (ClassNotFoundException e) {
            log.warn("事件类未找到: eventType={}", message.getEventType());
        } catch (Exception e) {
            log.error("事件反序列化失败: eventType={} eventId={}", message.getEventType(), message.getEventId(), e);
        }
        return null;
    }

    public static String truncate(String msg) {
        if (msg == null) {
            return "Unknown error";
        }
        return msg.length() > MAX_ERROR_MESSAGE_LENGTH ? msg.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...[truncated]" : msg;
    }
}
