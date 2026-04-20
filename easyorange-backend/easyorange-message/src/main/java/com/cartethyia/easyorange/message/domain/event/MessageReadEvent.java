package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageReadEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Message";

    private final Long messageId;
    private final Long readerId;

    public MessageReadEvent(Long messageId, Long readerId) {
        super(AGGREGATE_TYPE);
        this.messageId = messageId;
        this.readerId = readerId;
    }

    @Override
    public String eventType() {
        return "MessageRead";
    }
}
