package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "User";

    private final Long userId;
    private final String username;

    public UserRegisteredEvent(Long userId, String username) {
        super(AGGREGATE_TYPE);
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String eventType() {
        return "UserRegistered";
    }
}