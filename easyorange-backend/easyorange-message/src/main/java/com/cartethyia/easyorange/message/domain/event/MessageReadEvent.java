package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageReadEvent extends BaseDomainEvent {

    private final String messageId;
    private final String readerId;

    public MessageReadEvent(String messageId, String readerId) {
        super();
        this.messageId = messageId;
        this.readerId = readerId;
    }
}
