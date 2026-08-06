package com.cartethyia.easyorange.user.domain.event;

public record UserProfileUpdatedEvent(String userId) implements UserEvent {
    @Override
    public String userId() {
        return userId;
    }
}
