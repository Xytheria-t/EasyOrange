package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageDeletedEvent extends BaseDomainEvent {

    private final String messageId;
    private final String deleterId;

    public MessageDeletedEvent(String messageId, String deleterId) {
        super();
        this.messageId = messageId;
        this.deleterId = deleterId;
    }
}
