package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PasswordChangedEvent extends BaseDomainEvent {

    private final Long userId;

    public PasswordChangedEvent(Long userId) {
        super(PasswordChangedEvent.class);
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "PasswordChanged";
    }
}