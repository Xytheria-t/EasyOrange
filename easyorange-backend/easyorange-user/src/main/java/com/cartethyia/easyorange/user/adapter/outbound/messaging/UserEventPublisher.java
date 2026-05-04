package com.cartethyia.easyorange.user.adapter.outbound.messaging;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户事件发布器 - 适配器实现
 * <p>
 * 职责：
 * <ul>
 *   <li>实现 {@link UserEventPort} 接口</li>
 *   <li>委托给通用的 {@link DomainEventPublisher}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class UserEventPublisher implements UserEventPort {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void publish(BaseDomainEvent event) {
        domainEventPublisher.publish(event);
    }
}
