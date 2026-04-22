package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "User";

    private final UserId userId;
    private final String username;

    public UserRegisteredEvent(UserId userId, String username) {
        super(AGGREGATE_TYPE);
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String eventType() {
        return "UserRegistered";
    }
}