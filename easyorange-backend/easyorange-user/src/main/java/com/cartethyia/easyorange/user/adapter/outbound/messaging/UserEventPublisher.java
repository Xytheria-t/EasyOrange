package com.cartethyia.easyorange.user.adapter.outbound.messaging;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisher implements UserEventPort {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void publishUserRegistered(UserRegisteredEvent event) {
        domainEventPublisher.publish(event);
    }

    @Override
    public void publishPasswordChanged(PasswordChangedEvent event) {
        domainEventPublisher.publish(event);
    }

    @Override
    public void publishForgotPassword(ForgotPasswordEvent event) {
        domainEventPublisher.publish(event);
    }
}