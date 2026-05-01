package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import lombok.Getter;

@Getter
public class ForgotPasswordEvent extends BaseDomainEvent {

    private final Long userId;
    private final String phone;

    public ForgotPasswordEvent(Long userId, String phone) {
        super(User.class);
        this.userId = userId;
        this.phone = phone;
    }

    @Override
    public String eventType() {
        return "ForgotPassword";
    }
}
