package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class MessageDeletedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Message";

    private final Long messageId;
    private final Long deleterId;

    public MessageDeletedEvent(Long messageId, Long deleterId) {
        super(AGGREGATE_TYPE);
        this.messageId = messageId;
        this.deleterId = deleterId;
    }

    @Override
    public String eventType() {
        return "MessageDeleted";
    }
}
