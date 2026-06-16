package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageReadEvent extends BaseDomainEvent {

    private final Long messageId;
    private final Long readerId;

    public MessageReadEvent(Long messageId, Long readerId) {
        super();
        this.messageId = messageId;
        this.readerId = readerId;
    }
}
