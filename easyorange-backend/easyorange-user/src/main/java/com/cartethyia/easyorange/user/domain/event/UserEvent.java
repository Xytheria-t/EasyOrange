package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 用户领域事件密封接口 — 消除所有子类重复的 {@link #aggregateId()} 模板。
 */
public sealed interface UserEvent extends DomainEvent
    permits UserRegisteredEvent, UserProfileUpdatedEvent, UserPasswordChangedEvent, UserAvatarChangedEvent {

    String userId();

    @Override
    default String aggregateId() { return userId(); }
}
