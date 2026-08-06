package com.cartethyia.easyorange.user.domain.event;

public record UserRegisteredEvent(String userId, String username) implements UserEvent {
    @Override
    public String userId() {
        return userId;
    }
}
