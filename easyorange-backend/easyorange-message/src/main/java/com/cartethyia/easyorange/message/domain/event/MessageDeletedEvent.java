package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record MessageDeletedEvent(String messageId, String deleterId) implements DomainEvent {
    @Override
    public String aggregateId() {
        return messageId;
    }
}
