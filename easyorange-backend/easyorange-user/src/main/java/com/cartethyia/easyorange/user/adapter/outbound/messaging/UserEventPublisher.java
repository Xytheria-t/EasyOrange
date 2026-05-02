package com.cartethyia.easyorange.user.adapter.outbound.messaging;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisher implements UserEventPort {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void publishUserRegistered(Long userId, String username) {
        domainEventPublisher.publish(new UserRegisteredEvent(userId, username));
    }

    @Override
    public void publishPasswordChanged(Long userId) {
        domainEventPublisher.publish(new PasswordChangedEvent(userId));
    }

    @Override
    public void publishForgotPassword(Long userId, String phone) {
        domainEventPublisher.publish(new ForgotPasswordEvent(userId, phone));
    }
}
