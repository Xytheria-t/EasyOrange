package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageRecalledEvent extends BaseDomainEvent {

    private final String messageId;
    private final String conversationId;
    private final String operatorId;
    private final LocalDateTime recalledAt;

    public MessageRecalledEvent(String messageId, String conversationId, String operatorId, LocalDateTime recalledAt) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.operatorId = operatorId;
        this.recalledAt = recalledAt;
    }
}
