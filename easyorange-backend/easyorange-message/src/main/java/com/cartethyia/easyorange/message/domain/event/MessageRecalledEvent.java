package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import java.time.LocalDateTime;

public record MessageRecalledEvent(String messageId, String conversationId, String operatorId, LocalDateTime recalledAt)
        implements DomainEvent {
    @Override
    public String aggregateId() {
        return messageId;
    }
}
