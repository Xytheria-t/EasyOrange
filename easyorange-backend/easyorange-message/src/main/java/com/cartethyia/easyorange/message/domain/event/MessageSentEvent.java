package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageSentEvent extends BaseDomainEvent {

    private final Long messageId;
    private final Long senderId;
    private final Long receiverId;
    private final Integer type;

    public MessageSentEvent(Long messageId, Long senderId, Long receiverId, Integer type) {
        super();
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
    }
}
