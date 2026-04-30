package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.user.domain.model.User;
import lombok.Getter;

@Getter
public class PasswordChangedEvent extends BaseDomainEvent {

    private final Long userId;

    public PasswordChangedEvent(Long userId) {
        super(User.class);
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "PasswordChanged";
    }
}
