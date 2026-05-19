package com.cartethyia.easyorange.user.domain.port.output;

import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;

public interface UserEventPort extends OutboundPort {

    void publishUserRegistered(UserRegisteredEvent event);

    void publishPasswordChanged(PasswordChangedEvent event);

    void publishForgotPassword(ForgotPasswordEvent event);
}