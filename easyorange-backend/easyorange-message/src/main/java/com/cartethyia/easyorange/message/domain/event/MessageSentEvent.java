package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageSentEvent extends BaseDomainEvent {

    private final String messageId;
    private final String senderId;
    private final String receiverId;
    private final Integer type;

    public MessageSentEvent(String messageId, String senderId, String receiverId, Integer type) {
        super();
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
    }
}
