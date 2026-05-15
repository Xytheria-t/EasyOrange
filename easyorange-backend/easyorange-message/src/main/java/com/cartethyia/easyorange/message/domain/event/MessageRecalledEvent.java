package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageRecalledEvent extends BaseDomainEvent {

    private final Long messageId;
    private final String conversationId;
    private final Long operatorId;
    private final LocalDateTime recalledAt;

    public MessageRecalledEvent(Long messageId, String conversationId, Long operatorId, LocalDateTime recalledAt) {
        super(MessageRecalledEvent.class);
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.operatorId = operatorId;
        this.recalledAt = recalledAt;
    }

    @Override
    public String eventType() {
        return "MessageRecalled";
    }
}
