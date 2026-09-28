package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 用户领域事件密封接口 — 统一 {@link #aggregateId()} 委派给 {@link #userId()}，
 * 事件 record 只需声明一个 {@code userId} 分量即可满足 {@link DomainEvent} 契约。
 */
public sealed interface UserEvent extends DomainEvent permits UserPasswordChangedEvent, UserRegisteredEvent {

    String userId();

    @Override
    default String aggregateId() {
        return userId();
    }
}
