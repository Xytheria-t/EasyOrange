package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import lombok.Getter;

@Getter
public class PasswordChangedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "User";

    private final UserId userId;

    public PasswordChangedEvent(UserId userId) {
        super(AGGREGATE_TYPE);
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "PasswordChanged";
    }
}