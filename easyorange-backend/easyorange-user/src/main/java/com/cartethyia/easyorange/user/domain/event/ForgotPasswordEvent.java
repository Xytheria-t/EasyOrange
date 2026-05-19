package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import lombok.Getter;

@Getter
public class ForgotPasswordEvent extends BaseDomainEvent {

    private final Long userId;

    public ForgotPasswordEvent(Long userId) {
        super(User.class);
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "ForgotPassword";
    }
}
