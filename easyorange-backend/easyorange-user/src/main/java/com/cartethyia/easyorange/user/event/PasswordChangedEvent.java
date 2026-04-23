package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class PasswordChangedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "User";

    private final Long userId;

    public PasswordChangedEvent(Long userId) {
        super(AGGREGATE_TYPE);
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "PasswordChanged";
    }
}