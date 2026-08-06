package com.cartethyia.easyorange.user.domain.event;

public record UserAvatarChangedEvent(String userId) implements UserEvent {
    @Override
    public String userId() {
        return userId;
    }
}
